import { useEffect, useState } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import {
  authExpiredEvent,
  currentUser,
  hasAuthToken,
  isBackendConfigured,
  logout,
} from "./api";
import TopNav from "./components/TopNav";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import PantryPage from "./pages/PantryPage";
import ReceiptPage from "./pages/ReceiptPage";
import RecipeDetail from "./pages/RecipeDetail";
import RecipesPage from "./pages/RecipesPage";
import ReviewRecipesPage from "./pages/ReviewRecipesPage";
import ShoppingListPage from "./pages/ShoppingListPage";
import SignUpPage from "./pages/SignUpPage";
import SubmitRecipePage from "./pages/SubmitRecipePage";
import SuggestionsPage from "./pages/SuggestionsPage";

function normalizeRole(role) {
  const value = Array.isArray(role) ? role[0] : role;
  if (!value) return null;
  return value.startsWith("ROLE_") ? value : `ROLE_${value}`;
}

function connectionError(error) {
  if (error.code === "ECONNABORTED") {
    return "The free API took too long to wake up. Please try again.";
  }
  if (!error.response) {
    return "The free API is unavailable right now. Please try again in a minute.";
  }
  if (error.response.status === 401) {
    return "Your sign-in expired. Please log in again.";
  }
  return "I could not verify your sign-in. Please try again.";
}

export default function App() {
  const [role, setRole] = useState(null);
  const [loading, setLoading] = useState(
    () => isBackendConfigured && hasAuthToken()
  );
  const [authError, setAuthError] = useState("");

  useEffect(() => {
    const handleExpiredAuth = () => {
      setRole(null);
      setAuthError("Your sign-in expired. Please log in again.");
    };
    window.addEventListener(authExpiredEvent, handleExpiredAuth);
    return () => window.removeEventListener(authExpiredEvent, handleExpiredAuth);
  }, []);

  useEffect(() => {
    let active = true;

    async function restoreSession() {
      if (!isBackendConfigured) {
        setAuthError("The backend API is not configured for this deployment.");
        setLoading(false);
        return;
      }
      if (!hasAuthToken()) {
        setLoading(false);
        return;
      }

      try {
        const user = await currentUser();
        if (active) {
          setRole(normalizeRole(user.role));
          setAuthError("");
        }
      } catch (error) {
        if (active) setAuthError(connectionError(error));
      } finally {
        if (active) setLoading(false);
      }
    }

    restoreSession();
    return () => {
      active = false;
    };
  }, []);

  const handleLogin = (user) => {
    setRole(normalizeRole(user.role));
    setAuthError("");
  };

  const handleLogout = () => {
    logout();
    setRole(null);
    setAuthError("");
  };

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center px-6 text-center">
        <div>
          <p className="text-lg font-semibold text-slate-800">
            Connecting to Pantry Planner
          </p>
          <p className="mt-2 max-w-md text-sm text-slate-600">
            The free API can take about a minute to wake after it has been idle.
          </p>
        </div>
      </div>
    );
  }

  return (
    <>
      <TopNav role={role} onLogout={handleLogout} />
      <Routes>
        <Route
          path="/"
          element={
            role ? (
              <Navigate to="/suggestions" replace />
            ) : (
              <HomePage notice={authError} />
            )
          }
        />
        <Route
          path="/login"
          element={
            role ? (
              <Navigate to="/suggestions" replace />
            ) : (
              <LoginPage
                backendConfigured={isBackendConfigured}
                initialError={authError}
                onLogin={handleLogin}
              />
            )
          }
        />
        <Route
          path="/signup"
          element={
            role ? (
              <Navigate to="/suggestions" replace />
            ) : (
              <SignUpPage backendConfigured={isBackendConfigured} />
            )
          }
        />

        {role && (
          <>
            <Route path="/suggestions" element={<SuggestionsPage />} />
            <Route path="/pantry" element={<PantryPage />} />
            <Route path="/recipes" element={<RecipesPage />} />
            <Route path="/recipes/:id" element={<RecipeDetail />} />
            <Route path="/shopping-list" element={<ShoppingListPage />} />
            <Route path="/receipt/:id" element={<ReceiptPage />} />
          </>
        )}
        {role === "ROLE_USER" && (
          <Route path="/submit-recipe" element={<SubmitRecipePage />} />
        )}
        {role === "ROLE_ADMIN" && (
          <Route path="/review-recipes" element={<ReviewRecipesPage />} />
        )}
        <Route
          path="*"
          element={<Navigate to={role ? "/suggestions" : "/"} replace />}
        />
      </Routes>
    </>
  );
}
