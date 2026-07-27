import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import api from "../api";

export default function RecipeDetail() {
  const { id } = useParams();
  const [recipe, setRecipe] = useState(null);
  const [pantryIds, setPantryIds] = useState(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // ────────────────────────────────────────────────
  // Fetch recipe + current pantry
  // ────────────────────────────────────────────────
  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        setError("");
        const [r, p] = await Promise.all([
          api.get(`/recipes/${id}`),
          api.get("/pantry"),
        ]);
        setRecipe(r.data);
        setPantryIds(new Set(p.data.map((pi) => pi.ingredient.id)));
      } catch (e) {
        console.error("Error loading recipe details:", e?.response || e);
        setError("Failed to load recipe details.");
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [id]);

  // ────────────────────────────────────────────────
  // Add missing ingredients to shopping list
  // ────────────────────────────────────────────────
  const addMissing = async () => {
    try {
      await api.post(`/shopping-list/from-recipe/${id}`);
      alert("Missing ingredients added to shopping list!");
    } catch (e) {
      console.error("POST /shopping-list/from-recipe failed:", e?.response || e);
      alert("Could not add missing ingredients.");
    }
  };

  // ────────────────────────────────────────────────
  // Render
  // ────────────────────────────────────────────────
  if (loading) return <div className="p-4">Loading…</div>;
  if (error) return <div className="p-4 text-red-600">{error}</div>;
  if (!recipe) return <div className="p-4">Recipe not found.</div>;

  const hero = recipe.imageUrl || "/images/food-fallback-wide.jpg";

  return (
    <div className="p-4 max-w-3xl mx-auto">
      <div className="mb-4 rounded-xl overflow-hidden bg-slate-100">
        <img
          src={hero}
          alt={recipe.name}
          className="w-full h-56 sm:h-72 md:h-80 object-cover"
          loading="lazy"
          onError={(e) => {
            if (
              !e.currentTarget.src.endsWith("/images/food-fallback-wide.svg")
            ) {
              e.currentTarget.onerror = null;
              e.currentTarget.src = "/images/food-fallback-wide.svg";
            }
          }}
        />
      </div>

      <h1 className="text-2xl font-semibold mb-4">{recipe.name}</h1>

      <h2 className="font-semibold mb-2">Ingredients</h2>
      <ul className="mb-4 space-y-1">
        {(recipe.ingredients || []).map((ri) => {
          const have = pantryIds.has(ri.ingredient.id);
          return (
            <li key={ri.id} className="flex items-center gap-2">
              <span
                className={`px-2 py-0.5 rounded text-xs ${
                  have
                    ? "bg-green-100 text-green-700"
                    : "bg-red-100 text-red-700"
                }`}
              >
                {have ? "have" : "missing"}
              </span>
              <span className="capitalize">{ri.ingredient.name}</span>
            </li>
          );
        })}
      </ul>

      <div className="flex gap-2 mb-6">
        <button
          onClick={addMissing}
          className="inline-flex items-center gap-2 rounded-md bg-emerald-600 text-white px-3 py-2 hover:bg-emerald-700 transition"
        >
          Add missing to shopping list
        </button>
      </div>

      <h2 className="font-semibold mb-2">Instructions</h2>
      <p className="whitespace-pre-line leading-relaxed">
        {recipe.instructions || "No instructions provided yet."}
      </p>
    </div>
  );
}