import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api";

export default function ShoppingListPage() {
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  // -------------------------------------------------------------
  // 🛒 Load shopping list
  // -------------------------------------------------------------
  const load = async () => {
    try {
      setLoading(true);
      setError("");
      const res = await api.get("/shopping-list");
      setList(res.data || []);
    } catch (e) {
      console.error("❌ GET /shopping-list failed:", e?.response || e);
      setError("Failed to load shopping list. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  // -------------------------------------------------------------
  // ❌ Remove item
  // -------------------------------------------------------------
  const removeItem = async (id) => {
    if (!window.confirm("Remove this item from the list?")) return;
    try {
      await api.delete(`/shopping-list/${id}`);
      await load();
    } catch (e) {
      console.error("❌ DELETE /shopping-list failed:", e?.response || e);
      alert("Could not remove item.");
    }
  };

  // -------------------------------------------------------------
  // 💳 Buy now
  // -------------------------------------------------------------
  const buyNow = async (id, name) => {
    const priceInput = prompt(`Enter price for “${name}” (optional):`, "");
    const body = {};
    if (priceInput && !isNaN(parseFloat(priceInput))) {
      body.price = parseFloat(priceInput);
    }

    try {
      await api.post(`/shopping-list/${id}/purchase`, body);
      // ✅ After purchase, rebuild pantry and immediately go there
      await load();
      navigate("/pantry"); // direct navigation
    } catch (e) {
      console.error("❌ POST /shopping-list/{id}/purchase failed:", e?.response || e);
      alert("Purchase failed.");
    }
  };

  // -------------------------------------------------------------
  // 🖼️ Render
  // -------------------------------------------------------------
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col items-center p-6">
      <div className="max-w-4xl w-full bg-white rounded-lg shadow border border-slate-200 p-6">
        <header className="mb-5 flex items-center justify-between">
          <h1 className="text-2xl font-semibold text-slate-800">Shopping List</h1>
          <span className="text-sm text-slate-500">
            {list.length} {list.length === 1 ? "item" : "items"}
          </span>
        </header>

        {loading && <div className="text-slate-600">Loading items…</div>}
        {error && !loading && (
          <div className="text-red-600 border border-red-200 bg-red-50 p-2 rounded mb-4">
            {error}
          </div>
        )}

        {!loading && !error && (
          <>
            {list.length === 0 ? (
              <div className="text-slate-600">
                No items yet. Open a recipe and click “Add missing to shopping list.”
              </div>
            ) : (
              <ul className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {list.map((it) => (
                  <li
                    key={it.id}
                    className="flex flex-col justify-between rounded-lg border border-slate-200
                               bg-white p-4 shadow-sm hover:shadow-lg hover:-translate-y-0.5
                               transition"
                  >
                    <div>
                      <h3 className="capitalize font-medium text-slate-800 mb-1">
                        {it.ingredient?.name}
                      </h3>
                    </div>

                    <div className="flex justify-between items-center mt-3">
                      <button
                        onClick={() => buyNow(it.id, it.ingredient?.name)}
                        className="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-1.5
                                   rounded-md text-sm transition"
                      >
                        ✅ Buy now
                      </button>
                      <button
                        onClick={() => removeItem(it.id)}
                        className="border border-red-300 text-red-600 hover:bg-red-50
                                   px-3 py-1.5 rounded-md text-sm transition"
                      >
                        Remove
                      </button>
                    </div>
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