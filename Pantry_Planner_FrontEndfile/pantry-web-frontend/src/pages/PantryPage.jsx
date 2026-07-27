import { useEffect, useRef, useState } from "react";
import api from "../api";

export default function PantryPage() {
  const [items, setItems] = useState([]);
  const [q, setQ] = useState("");
  const [suggestions, setSuggestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  const [showDropdown, setShowDropdown] = useState(false);
  const inputRef = useRef(null);

  // -------------------------------------------------------------
  // 🥫 Load pantry items on mount
  // -------------------------------------------------------------
  const loadPantry = async () => {
    try {
      setLoading(true);
      setErr("");
      const res = await api.get("/pantry");
      setItems(Array.isArray(res.data) ? res.data : []);
    } catch (e) {
      console.error("❌ GET /pantry failed:", e?.response || e);
      setErr("Failed to load pantry. Please refresh or try again later.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPantry();
  }, []);

  // -------------------------------------------------------------
  // 🌿 Ingredient auto‑complete
  // -------------------------------------------------------------
  useEffect(() => {
    const t = setTimeout(async () => {
      const term = q.trim();
      if (!term) {
        setSuggestions([]);
        setShowDropdown(false);
        return;
      }

      try {
        const res = await api.get("/ingredients", { params: { search: term } });
        setSuggestions(Array.isArray(res.data) ? res.data : []);
        setShowDropdown(true);
      } catch (e) {
        console.error("❌ GET /ingredients failed:", e?.response || e);
        setSuggestions([]);
        setShowDropdown(false);
      }
    }, 250);

    return () => clearTimeout(t);
  }, [q]);

  // -------------------------------------------------------------
  // ➕ Add ingredient to pantry
  // -------------------------------------------------------------
  const addItem = async (name) => {
    const clean = name.trim();
    if (!clean) return;

    const alreadyExists = items.some(
      (it) => it.ingredient?.name?.toLowerCase() === clean.toLowerCase()
    );
    if (alreadyExists) {
      setQ("");
      setSuggestions([]);
      setShowDropdown(false);
      return;
    }

    try {
      await api.post("/pantry", { ingredientName: clean });
      setQ("");
      setSuggestions([]);
      setShowDropdown(false);
      await loadPantry();
    } catch (e) {
      console.error("❌ POST /pantry failed:", e?.response || e);
      alert("Failed to add item. Please try again.");
    }
  };

  // -------------------------------------------------------------
  // ❌ Remove pantry item
  // -------------------------------------------------------------
  const removeItem = async (id) => {
    if (!window.confirm("Remove this item from pantry?")) return;
    const prev = [...items];
    setItems((p) => p.filter((x) => x.id !== id));

    try {
      await api.delete(`/pantry/${id}`);
    } catch (e) {
      console.error("❌ DELETE /pantry failed:", e?.response || e);
      alert(`Failed to remove the item (status ${e?.response?.status ?? "?"}).`);
      setItems(prev); // rollback on error
    }
  };

  // -------------------------------------------------------------
  // ⌨️ Keyboard handling
  // -------------------------------------------------------------
  const onKeyDown = (e) => {
    if (e.key === "Enter") {
      e.preventDefault();
      addItem(q);
    } else if (e.key === "Escape") {
      setShowDropdown(false);
    }
  };

  // -------------------------------------------------------------
  // 🖼️ Render UI
  // -------------------------------------------------------------
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col items-center p-6">
      <div className="w-full max-w-4xl bg-white rounded-lg shadow border border-slate-200 p-6">
        <header className="mb-5 flex items-center justify-between">
          <h1 className="text-2xl font-semibold text-slate-800">Pantry</h1>
          <div className="text-sm text-slate-500">
            {items.length} item{items.length !== 1 ? "s" : ""}
          </div>
        </header>

        {/* Input and dropdown */}
        <div className="mb-6 relative">
          <input
            ref={inputRef}
            className="border border-slate-300 rounded-lg w-full p-2.5 shadow-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            placeholder="Add ingredient (tomato, pasta, egg…)"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onFocus={() => q.trim() && setShowDropdown(true)}
            onKeyDown={onKeyDown}
          />

          {showDropdown && suggestions.length > 0 && (
            <div className="absolute z-20 mt-1 w-full bg-white border border-slate-200 rounded-lg shadow">
              {suggestions.map((s) => (
                <button
                  key={s.id || s.name}
                  type="button"
                  className="w-full text-left px-3 py-2 hover:bg-slate-100 capitalize"
                  onClick={() => addItem(s.name)}
                >
                  {s.name}
                </button>
              ))}
              <button
                type="button"
                className="w-full text-left px-3 py-2 text-emerald-700 hover:bg-slate-100 border-t border-slate-200"
                onClick={() => addItem(q)}
              >
                Add “{q}”
              </button>
            </div>
          )}
        </div>

        {loading && <div className="text-slate-600">Loading your pantry…</div>}
        {err && !loading && <div className="text-red-600">{err}</div>}

        {/* Pantry list */}
        {!loading && !err && (
          <>
            {items.length === 0 ? (
              <div className="text-slate-600">
                Your pantry is empty. Add some ingredients above!
              </div>
            ) : (
              <ul className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {items.map((it) => (
                  <li
                    key={it.id}
                    className="flex items-center justify-between rounded-lg border border-slate-200 bg-white p-4 shadow-sm hover:shadow-lg transition hover:-translate-y-0.5"
                  >
                    <span className="capitalize font-medium text-slate-800">
                      {it.ingredient?.name}
                    </span>
                    <button
                      className="border border-red-300 text-red-600 px-3 py-1.5 rounded-md text-sm hover:bg-red-50 transition"
                      onClick={() => removeItem(it.id)}
                    >
                      Remove
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </>
        )}
      </div>
    </div>
  );
}