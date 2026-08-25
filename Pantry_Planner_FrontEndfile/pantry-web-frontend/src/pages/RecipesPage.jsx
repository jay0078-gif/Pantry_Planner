import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api";
import { publicAsset, resolveImageUrl } from "../lib/images";

export default function RecipesPage() {
  const [recipes, setRecipes] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const navigate = useNavigate();
  const fallbackImage = publicAsset("images/food-fallback.svg");

  // ------------------------------------------------------------------
  // 🔹 Fetch all or filtered recipes
  // ------------------------------------------------------------------
  async function loadRecipes(term = "") {
    setLoading(true);
    setError(null);
    try {
      // ✅ Correct endpoint (no duplicated /api)
      const res = await api.get("/recipes", { params: { search: term || undefined } });

      const data = Array.isArray(res.data)
        ? res.data
        : Array.isArray(res.data?.data)
        ? res.data.data
        : [];
      setRecipes(data);
    } catch (err) {
      console.error("❌  GET /recipes failed:", err);
      const msg =
        err?.response?.data?.error ||
        err?.response?.data?.message ||
        err?.response?.statusText ||
        err.message ||
        "Server error fetching recipes.";
      setError(`Backend error: ${msg}`);
      setRecipes([]);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadRecipes();
  }, []);

  // Debounce search
  useEffect(() => {
    const id = setTimeout(() => loadRecipes(search.trim()), 400);
    return () => clearTimeout(id);
  }, [search]);

  // ------------------------------------------------------------------
  // 🖼️ Render UI
  // ------------------------------------------------------------------
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col items-center p-6">
      <div className="w-full max-w-6xl bg-white border border-slate-200 shadow-lg rounded-lg p-6">
        {/* Header */}
        <header className="mb-6 flex flex-wrap justify-between items-center gap-3">
          <h1 className="text-3xl font-bold text-slate-800 tracking-tight">Recipes</h1>
          <span className="text-sm text-slate-500">
            {recipes.length} {recipes.length === 1 ? "recipe" : "recipes"}
          </span>
        </header>

        {/* 🔎 Search */}
        <div className="flex flex-col sm:flex-row gap-2 mb-6">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search recipes or ingredients..."
            className="border border-slate-300 rounded-lg p-2 flex-1 shadow-sm
                       focus:outline-none focus:ring-2 focus:ring-emerald-500"
          />
          <button
            onClick={() => loadRecipes(search.trim())}
            disabled={loading}
            className="bg-emerald-600 hover:bg-emerald-700 text-white px-5 py-2 rounded-lg
                       disabled:opacity-60 transition duration-200"
          >
            {loading ? "Loading…" : "Search"}
          </button>
        </div>

        {/* ⚠️ Error or Loading */}
        {error && (
          <div className="border border-red-200 bg-red-50 text-red-700 p-3 rounded mb-4 text-sm">
            {error}
          </div>
        )}
        {loading && !error && (
          <p className="text-slate-500 text-center mb-4">Fetching recipes…</p>
        )}

        {/* 📜 Recipe List */}
        {!loading && !error && (
          <>
            {recipes.length === 0 ? (
              <p className="text-slate-500 text-center py-10 text-lg">
                No recipes found. Try a different search.
              </p>
            ) : (
              <ul className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-7">
                {recipes.map((r) => (
                  <li
                    key={r.id}
                    onClick={() => navigate(`/recipes/${r.id}`)}
                    className="group cursor-pointer border border-slate-200 rounded-xl bg-white overflow-hidden
                               shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-200"
                  >
                    {/* Image */}
                    <img
                      src={resolveImageUrl(r.imageUrl)}
                      alt={r.name}
                      className="w-full h-48 object-cover group-hover:scale-105 transition-transform duration-300"
                      onError={(event) => {
                        event.currentTarget.onerror = null;
                        event.currentTarget.src = fallbackImage;
                      }}
                    />

                    {/* Content */}
                    <div className="p-4">
                      <h2 className="font-semibold text-lg capitalize text-emerald-700 mb-2 group-hover:text-emerald-800 transition-colors">
                        {r.name || "Untitled Recipe"}
                      </h2>

                      <p className="text-sm text-slate-600 leading-relaxed">
                        {r.instructions && r.instructions.length > 120
                          ? r.instructions.slice(0, 120) + "…"
                          : r.instructions || "No instructions provided yet."}
                      </p>
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
