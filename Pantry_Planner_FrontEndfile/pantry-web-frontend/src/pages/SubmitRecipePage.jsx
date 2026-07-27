import { useState } from "react";
import { toast } from "react-toastify";
import api from "../api";

export default function SubmitRecipePage() {
  const [title, setTitle] = useState("");
  const [ingredients, setIngredients] = useState("");
  const [instructions, setInstructions] = useState("");
  const [loading, setLoading] = useState(false);

  // 🔹 Handle form submit
  async function handleSubmit(e) {
    e.preventDefault();

    if (!title.trim() || !instructions.trim()) {
      toast.warn("Please provide both title and instructions.");
      return;
    }

    const payload = {
      title: title.trim(),
      ingredients: ingredients
        .split(",")
        .map((s) => s.trim())
        .filter(Boolean),
      instructions: instructions.trim(),
    };

    try {
      setLoading(true);
      // ✅ FIX: remove extra `/api` — baseURL already includes it
      await api.post("/user/recipes", payload);

      toast.success("✅ Recipe submitted for approval!");
      setTitle("");
      setIngredients("");
      setInstructions("");
    } catch (err) {
      console.error("❌ Submit recipe failed:", err?.response || err);
      toast.error(
        err.response?.data?.error ||
          err.response?.data?.message ||
          "❌ Failed to submit recipe."
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="max-w-3xl mx-auto p-6">
      <h1 className="text-2xl font-semibold mb-4">Submit Your Recipe</h1>

      <form
        onSubmit={handleSubmit}
        className="bg-white p-4 rounded shadow space-y-4"
      >
        <div>
          <label className="block text-sm font-medium mb-1">Title</label>
          <input
            type="text"
            className="border rounded w-full p-2"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="E.g. Creamy Mushroom Pasta"
          />
        </div>

        <div>
          <label className="block text-sm font-medium mb-1">
            Ingredients (comma‑separated)
          </label>
          <input
            type="text"
            className="border rounded w-full p-2"
            value={ingredients}
            onChange={(e) => setIngredients(e.target.value)}
            placeholder="Mushrooms, Garlic, Cream, Pasta"
          />
        </div>

        <div>
          <label className="block text-sm font-medium mb-1">Instructions</label>
          <textarea
            rows={6}
            className="border rounded w-full p-2"
            value={instructions}
            onChange={(e) => setInstructions(e.target.value)}
            placeholder="Step 1: Cook pasta...\nStep 2: Sauté garlic..."
          />
        </div>

        <div className="flex justify-end">
          <button
            type="submit"
            disabled={loading}
            className="bg-emerald-600 text-white px-4 py-2 rounded
                       hover:bg-emerald-700 transition disabled:opacity-60"
          >
            {loading ? "Submitting…" : "Submit"}
          </button>
        </div>
      </form>
    </div>
  );
}