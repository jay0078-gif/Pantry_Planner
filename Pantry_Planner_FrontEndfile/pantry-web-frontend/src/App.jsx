import { useEffect, useState } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import api, { logout as apiLogout } from "./api";

import TopNav from "./components/TopNav";

// Main pages
import SuggestionsPage from "./pages/SuggestionsPage";
import PantryPage from "./pages/PantryPage";
import RecipesPage from "./pages/RecipesPage";
import RecipeDetail from "./pages/RecipeDetail";
import ShoppingListPage from "./pages/ShoppingListPage";
import ReceiptPage from "./pages/ReceiptPage";

// Role‑specific pages
import SubmitRecipePage from "./pages/SubmitRecipePage";
import ReviewRecipesPage from "./pages/ReviewRecipesPage";

export default function App() {
  const [role, setRole] = useState(null);
  const [loading, setLoading] = useState(true);
  const [authError, setAuthError] = useState(null);

  // ------------------------------------------------------------------
  // 🔹 Fetch current authenticated user
  // ------------------------------------------------------------------
  useEffect(() => {
    const fetchCurrentUser = async () => {
      try {
        const res = await api.get("/auth/current");
        console.log("🧠 User role from backend:", res.data.role);

        // Normalize whatever backend sends into a single string
        let currentRole = res.data.role;
        if (Array.isArray(currentRole)) currentRole = currentRole[0];
        if (currentRole && !currentRole.startsWith("ROLE_")) {
          currentRole = "ROLE_" + currentRole;
        }
        setRole(currentRole);
      } catch (err) {
        console.error("❌  GET /auth/current failed:", err);
        setAuthError("Not logged in or session expired.");
      } finally {
        setLoading(false);
      }
    };

    fetchCurrentUser();
  }, []);

  // ------------------------------------------------------------------
  // 🔸 Loading & unauthenticated states
  // ------------------------------------------------------------------
  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen text-lg font-medium">
        Loading…
      </div>
    );
  }

  if (!role) {
    return (
      <div className="flex flex-col items-center justify-center h-screen text-center">
        <h2 className="text-2xl font-semibold mb-2">Please log in</h2>
        <p className="text-red-600 mb-4">{authError}</p>
        <a
          href="http://localhost:8080/login"
          className="text-green-700 underline font-medium"
        >
          Open login page
        </a>
      </div>
    );
  }

  // ------------------------------------------------------------------
  // 🔹 Logout handler
  // ------------------------------------------------------------------
  const handleLogout = async () => {
    try {
      await apiLogout();
    } catch (e) {
      console.warn("Logout request failed:", e);
    } finally {
      localStorage.removeItem("token");
      window.location.href = "http://localhost:8080/login";
    }
  };

  // ------------------------------------------------------------------
  // 🌍 Render authenticated area
  // ------------------------------------------------------------------
  return (
    <>
      <TopNav role={role} onLogout={handleLogout} />

      <Routes>
        <Route path="/" element={<SuggestionsPage />} />
        <Route path="/pantry" element={<PantryPage />} />
        <Route path="/recipes" element={<RecipesPage />} />
        <Route path="/recipes/:id" element={<RecipeDetail />} />
        <Route path="/shopping-list" element={<ShoppingListPage />} />
        <Route path="/receipt/:id" element={<ReceiptPage />} />

        {/* Role‑specific routes */}
        {role === "ROLE_USER" && (
          <Route path="/submit-recipe" element={<SubmitRecipePage />} />
        )}
        {["ROLE_OWNER", "ROLE_ADMIN"].includes(role) && (
          <Route path="/review-recipes" element={<ReviewRecipesPage />} />
        )}

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  );
}