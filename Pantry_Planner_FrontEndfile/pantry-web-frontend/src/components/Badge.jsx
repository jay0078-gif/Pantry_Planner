export default function Badge({ color = "slate", children }) {
const cls =
color === "green" ? "bg-green-100 text-green-700" :
color === "red" ? "bg-red-100 text-red-700" :
"bg-slate-100 text-slate-700";
return <span className={`px-2 py-0.5 rounded text-xs ${cls}`}>{children}</span>;
}