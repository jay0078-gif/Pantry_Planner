import { Link, NavLink, useNavigate } from "react-router-dom";
import Logo from "./Logo";

const linkCls = ({ isActive }) =>
  `inline-flex min-h-11 shrink-0 items-center whitespace-nowrap px-3 py-1.5 rounded-md text-sm sm:text-base font-medium
   transition-all duration-200 ${
     isActive
       ? "bg-black/15 text-white shadow-sm"
       : "text-white/90 hover:bg-black/10 hover:text-white"
   }`;

export default function TopNav({ role, onLogout }) {
  const navigate = useNavigate();

  const handleLogout = () => {
    onLogout();
    navigate("/", { replace: true });
  };

  return (
    <header className="sticky top-0 z-40 shadow-md">
      <div className="bg-mint-gradient text-white">
        <div className="mx-auto flex min-h-16 max-w-7xl items-center justify-between gap-3 px-4 py-2.5 sm:px-6 lg:px-8">
          <Link
            to="/"
            className="flex shrink-0 items-center gap-3 rounded-md transition-opacity hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white"
            aria-label="Pantry Planner Home"
          >
            <Logo
              size={40}
              label="Pantry Planner"
              labelClass="hidden text-xl font-semibold tracking-tight sm:inline sm:text-2xl"
            />
          </Link>

          {role ? (
            <div className="flex min-w-0 items-center gap-1 sm:gap-2">
              <nav
                className="no-scrollbar flex min-w-0 items-center gap-1 overflow-x-auto sm:gap-2"
                aria-label="Planner navigation"
              >
                <NavLink to="/suggestions" className={linkCls}>
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

                {role === "ROLE_USER" && (
                  <NavLink to="/submit-recipe" className={linkCls}>
                    Submit
                  </NavLink>
                )}
                {role === "ROLE_ADMIN" && (
                  <NavLink to="/review-recipes" className={linkCls}>
                    Review (Admin)
                  </NavLink>
                )}
              </nav>

              <button
                onClick={handleLogout}
                className="min-h-11 shrink-0 rounded-md bg-white px-3 py-2 text-sm font-semibold text-emerald-800 transition-colors duration-200 hover:bg-emerald-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white sm:px-4 sm:text-base"
                type="button"
              >
                Log out
              </button>
            </div>
          ) : (
            <nav
              className="flex shrink-0 items-center gap-2 sm:gap-3"
              aria-label="Account navigation"
            >
              <NavLink
                to="/login"
                className="inline-flex min-h-11 items-center rounded-lg px-3 text-sm font-semibold text-white transition hover:bg-black/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white sm:px-4 sm:text-base"
              >
                Log in
              </NavLink>
              <NavLink
                to="/signup"
                className="inline-flex min-h-11 items-center rounded-lg bg-white px-3.5 text-sm font-semibold text-emerald-800 shadow-sm transition hover:bg-emerald-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white sm:px-5 sm:text-base"
              >
                Sign up
              </NavLink>
            </nav>
          )}
        </div>
      </div>
    </header>
  );
}
