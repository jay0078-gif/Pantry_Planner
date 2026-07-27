import { useEffect, useState } from "react";

export default function ThemeToggle({ className = "" }) {
const [theme, setTheme] = useState(() => {
const saved = localStorage.getItem("theme");
if (saved === "light" || saved === "dark") return saved;
return window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches
? "dark"
: "light";
});

useEffect(() => {
const root = document.documentElement;
if (theme === "dark") root.classList.add("dark");
else root.classList.remove("dark");
localStorage.setItem("theme", theme);
}, [theme]);

const toggle = () => setTheme(t => (t === "dark" ? "light" : "dark"));

return (
<button
  onClick={toggle}
  className={`inline-flex items-center gap-2 px-3 py-1.5 rounded border border-slate-200 hover:bg-slate-100 bg-white ${className}`}
  title="Toggle theme"
>
<span className="text-sm">{theme === "dark" ? "Light" : "Dark"} mode</span>
<span aria-hidden>{theme === "dark" ? "🌞" : "🌙"}</span>
</button>
);
}

