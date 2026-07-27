import { useEffect, useState } from "react";
import api from "../api";

export default function RecipesPage() {
  const [recipes, setRecipes] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // ------------------------------------------------------------------
  // 🔹 Fetch all or filtered recipes from backend
  // ------------------------------------------------------------------
  const loadRecipes = async (term = "") => {
    try {
      setLoading(true);
      setError(null);

      // ✅  Always call the *real* backend endpoint
      const res = await api.get("/api/recipes", {
        params: { search: term },
      });

      // Normalize/guard the response
      if (Array.isArray(res.data)) {
        setRecipes(res.data);
      } else if (Array.isArray(res.data.data)) {
        // some APIs wrap payload under a "data" key
        setRecipes(res.data.data);
      } else {
        setRecipes([]);
      }
    } catch (err) {
      console.error("❌  GET /api/recipes failed:", err);
      const msg =
        err?.response?.data?.error ||
        err?.response?.statusText ||
        "Server error while loading recipes.";
      setError(`Backend error: ${msg}`);
      setRecipes([]);
    } finally {
      setLoading(false);
    }
  };

  // ------------------------------------------------------------------
  // 🔸 Load recipes on first mount
  // ------------------------------------------------------------------
  useEffect(() => {
    loadRecipes();
  }, []);

  // ------------------------------------------------------------------
  // 🔎 Debounced live search
  // ------------------------------------------------------------------
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      loadRecipes(search.trim());
    }, 400);
    return () => clearTimeout(timeoutId);
  }, [search]);

  // ------------------------------------------------------------------
  // 🧱 Render
  // ------------------------------------------------------------------
  return (
    <div className="p-4">
      <h1 className="text-2xl font-semibold mb-4">Recipes</h1>

      {/* 🔍 Search Bar -------------------------------------------------- */}
      <div className="flex items-center gap-2 mb-4">
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search recipes or ingredients"
          className="border border-slate-300 rounded p-2 w-72 shadow-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
        />
        <button
          onClick={() => loadRecipes(search.trim())}
          disabled={loading}
          className="bg-emerald-600 text-white px-4 py-1.5 rounded hover:bg-emerald-700 disabled:opacity-60"
        >
          {loading ? "Loading…" : "Search"}
        </button>
      </div>

      {/* ⚠ Error & Loading States ------------------------------------ */}
      {error && <div className="text-red-600 mb-2">{error}</div>}
      {loading && <div className="text-slate-600">Loading recipes…</div>}

      {/* 📜 Result List ---------------------------------------------- */}
      {!loading && !error && (
        <div>
          {recipes.length === 0 ? (
            <p className="text-slate-600">
              No recipes found. Try searching different keywords.
            </p>
          ) : (
            <ul className="space-y-4">
              {recipes.map((r) => (
                <li
                  key={r.id}
                  className="rounded border border-slate-200 bg-white p-3 shadow-sm hover:shadow-md transition"
                >
                  <div className="font-semibold text-lg capitalize">
                    {r.name || "Untitled Recipe"}
                  </div>

                  {r.instructions && (
                    <div className="text-sm text-slate-600 mt-1">
                      {r.instructions.length > 120
                        ? r.instructions.slice(0, 120) + "…"
                        : r.instructions}
                    </div>
                  )}

                  {r.imageUrl && (
                    <img
                      src={r.imageUrl}
                      alt={r.name}
                      className="mt-2 rounded-md max-h-48 object-cover"
                    />
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}