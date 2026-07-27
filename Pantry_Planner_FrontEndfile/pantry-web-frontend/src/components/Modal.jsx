import { useEffect } from "react";

export default function Modal({ open, onClose, title, children, actions }) {
useEffect(() => {
const onKey = (e) => e.key === "Escape" && onClose?.();
document.addEventListener("keydown", onKey);
return () => document.removeEventListener("keydown", onKey);
}, [onClose]);

if (!open) return null;
return (
<div className="fixed inset-0 z-50 flex items-center justify-center">
<div className="absolute inset-0 bg-black/40" onClick={onClose} />
<div className="relative w-full max-w-md mx-3 rounded-lg bg-white border border-slate-200 p-4 dark:bg-slate-900 dark:border-slate-700">
{title && <h3 className="text-lg font-semibold mb-2">{title}</h3>}
<div className="mb-4">{children}</div>
<div className="flex justify-end gap-2">{actions}</div>
</div>
</div>
);
}