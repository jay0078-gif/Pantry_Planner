// src/components/Logo.jsx
export default function Logo({
  size = 46,
  label = "Pantry Planner",
  labelClass = "",
}) {
  return (
    <div className="flex items-center gap-2 leading-none select-none">
      <div
        style={{ width: size, height: size }}
        className="flex items-center justify-center rounded-full bg-white shadow-md ring-1 ring-[#e6e6e6]"
      >
        <img
          src="/logo.svg"
          alt="Pantry Planner logo"
          className="object-contain w-[75%] h-[75%]"
        />
      </div>
      {label && <span className={labelClass}>{label}</span>}
    </div>
  );
}