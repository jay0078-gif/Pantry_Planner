import { useEffect, useState } from "react";
import api from "../api";
import RecipeCard from "../components/RecipeCard";

export default function SuggestionsPage() {
  const [maxMissing, setMaxMissing] = useState(2);
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // -----------------------------------------------------------------
  // 🔹 Fetch recipe suggestions from backend
  // -----------------------------------------------------------------
  const loadSuggestions = async () => {
    try {
      setLoading(true);
      setError(null);

      // ✅ baseURL in api.js already ends with /api → call only "/suggestions"
      const res = await api.get("/suggestions", {
        params: { maxMissing, limit: 50 },
      });

      setList(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      console.error("❌ Failed to load suggestions:", err);
      const msg =
        err?.response?.data?.error ||
        err?.response?.statusText ||
        "Could not load recipe suggestions. Please try again.";
      setError(msg);
      setList([]);
    } finally {
      setLoading(false);
    }
  };

  // -----------------------------------------------------------------
  // 🔸 Reload whenever maxMissing changes
  // -----------------------------------------------------------------
  useEffect(() => {
    loadSuggestions();
  }, [maxMissing]);

  // -----------------------------------------------------------------
  // 🖼 Render
  // -----------------------------------------------------------------
  return (
    
    <div className="min-h-screen bg-slate-50 flex flex-col items-center p-6">
      {/* Header + filter */}
      <div className="flex items-center gap-3 mb-4">
        <h1 className="text-2xl font-semibold" style={{ marginRight: "669px" }}>Suggestions</h1>

        <select
          className="border rounded p-2"
          value={maxMissing}
          onChange={(e) => setMaxMissing(Number(e.target.value))}
        >
          <option value={0}>Cook now (0 missing)</option>
          <option value={1}>Almost there (≤ 1 missing)</option>
          <option value={2}>Show more (≤ 2 missing)</option>
        </select>

        <button
          onClick={loadSuggestions}
          disabled={loading}
          className="bg-emerald-600 text-white px-3 py-1.5 rounded hover:bg-emerald-700 disabled:opacity-60"
        >
          {loading ? "Loading…" : "Refresh"}
        </button>
      </div>

      {/* ⚙ Error / Loading / List */}
      {error && <div className="text-red-600 mb-2">{error}</div>}
      {loading && <div className="text-slate-600">Loading recipes…</div>}

      {!loading && !error && (
        <>
          {list.length === 0 ? (
            <p className="text-slate-600">
              No recipes match your pantry. Add items or allow more missing
              ingredients.
            </p>
          ) : (
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {list.map((s) => (
                <RecipeCard key={s.recipeId} s={s} />
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}