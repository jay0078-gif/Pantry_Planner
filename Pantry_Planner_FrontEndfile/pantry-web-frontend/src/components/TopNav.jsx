import { Link, NavLink, useNavigate } from "react-router-dom";
import Logo from "./Logo";

const linkCls = ({ isActive }) =>
  `px-3 py-1.5 rounded-md text-sm sm:text-base font-medium
   transition-all duration-200 ${
     isActive
       ? "bg-white/25 text-white shadow-sm"
       : "text-white/90 hover:bg-white/15 hover:text-white"
   }`;

export default function TopNav({ role, onLogout }) {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem("token");
    sessionStorage.clear();
    if (onLogout) onLogout();
    else navigate("/login");
  };

  return (
    <header className="sticky top-0 z-40 shadow-md">
      <div className="bg-mint-gradient text-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          {/* Brand */}
          <Link
            to="/"
            className="flex items-center gap-3 hover:opacity-90 transition-opacity"
            aria-label="Pantry Planner Home"
          >
            <Logo
              size={40}
              label="Pantry Planner"
              labelClass="text-2xl font-semibold tracking-tight"
              variant="white"
            />
          </Link>

          {/* Navigation links */}
          <nav className="flex flex-wrap items-center gap-1 sm:gap-2 justify-center sm:justify-end">
            <NavLink to="/" className={linkCls}>
              Suggestions
            </NavLink>
            <NavLink to="/pantry" className={linkCls}>
              Pantry
            </NavLink>
            <NavLink to="/recipes" className={linkCls}>
              Recipes
            </NavLink>
            <NavLink to="/shopping-list" className={linkCls}>
              Shopping List
            </NavLink>

            {/* Role‑specific buttons */}
            {role === "ROLE_USER" && (
              <NavLink to="/submit-recipe" className={linkCls}>
                Submit
              </NavLink>
            )}
            {role === "ROLE_OWNER" && (
              <NavLink to="/review-recipes" className={linkCls}>
                Review
              </NavLink>
            )}
            {role === "ROLE_ADMIN" && (
              <NavLink to="/review-recipes" className={linkCls}>
                Review (Admin)
              </NavLink>
            )}

            {/* Logout */}
            <button
              onClick={handleLogout}
              className="ml-2 px-4 py-1.5 rounded-md text-sm sm:text-base font-semibold
                         bg-white text-[#3eb489] hover:bg-[#3eb489] hover:text-white
                         transition-colors duration-200"
            >
              Logout
            </button>
          </nav>
        </div>
      </div>
    </header>
  );
}