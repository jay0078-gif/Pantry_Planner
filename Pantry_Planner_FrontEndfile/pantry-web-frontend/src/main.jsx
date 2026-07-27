import React, { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

import "./index.css";
import App from "./App.jsx";

// --- App pages -----------------------------------------------------------
import SuggestionsPage from "./pages/SuggestionsPage.jsx";
import PantryPage from "./pages/PantryPage.jsx";
import RecipesPage from "./pages/RecipesPage.jsx";
import RecipeDetail from "./pages/RecipeDetail.jsx";
import ShoppingListPage from "./pages/ShoppingListPage.jsx";
import ReceiptPage from "./pages/ReceiptPage.jsx";

// --- Owner User portals -----------------------------------------------
import SubmitRecipePage from "./pages/SubmitRecipePage.jsx";
import ReviewRecipesPage from "./pages/ReviewRecipesPage.jsx";

// --- Router configuration ------------------------------------------------
const router = createBrowserRouter([
  {
      path: "*",              // parent route and layout host
    element: <App />,
    children: [
      // public / default
      { index: true, element: <SuggestionsPage /> },

      // pantry & recipes
      { path: "pantry", element: <PantryPage /> },
      { path: "recipes", element: <RecipesPage /> },
      { path: "recipes/:id", element: <RecipeDetail /> },

      // shopping & receipts
      { path: "shopping-list", element: <ShoppingListPage /> },
      { path: "receipt/:id", element: <ReceiptPage /> },

      // owner / user portals
      { path: "submit-recipe", element: <SubmitRecipePage /> },
      { path: "review-recipes", element: <ReviewRecipesPage /> },

      // fallback 404
      {
        path: "*",
        element: (
          <h2 style={{ padding: 20, textAlign: "center" }}>
            404 Page Not Found
          </h2>
        ),
      },
    ],
  },
]);

// --- Mount React ---------------------------------------------------------
createRoot(document.getElementById("root")).render(
  <StrictMode>
    <RouterProvider router={router} />
    {/* toast notifications (bottom‑right, short auto‑close) */}
    <ToastContainer position="bottom-right" autoClose={2500} />
  </StrictMode>
);