import Card from "./Card";
import { Link } from "react-router-dom";
import RecipePhoto from "./RecipePhoto";

export default function RecipeCard({ s }) {
const pct = s.totalIngredients ? Math.round((s.matched / s.totalIngredients) * 100) : 0;

return (
<Card>
<RecipePhoto
  photo={s}
  containerClassName="w-full h-48 overflow-hidden"
  imageClassName="w-full h-full object-cover"
  attributionClassName="px-4 pt-2"
  loading="lazy"
/>

<div className="p-4">
    <div className="flex items-start justify-between">
      <div>
        <Link to={`/recipes/${s.recipeId}`} className="font-semibold hover:underline">
          {s.name}
        </Link>
        <div className="mt-1 text-sm text-slate-600">
          {s.matched}/{s.totalIngredients} available • Missing: {s.missing}
        </div>
      </div>
      <div className="text-sm text-slate-600">{pct}%</div>
    </div>

    {s.missingIngredients.length > 0 && (
      <div className="text-sm text-slate-700 mt-2">
        Missing: {s.missingIngredients.join(", ")}
      </div>
    )}

    <div className="mt-3 h-2 bg-slate-200 rounded">
      <div className="h-2 rounded bg-emerald-600" style={{ width: `${pct}%` }} />
    </div>
  </div>
</Card>
);
}
