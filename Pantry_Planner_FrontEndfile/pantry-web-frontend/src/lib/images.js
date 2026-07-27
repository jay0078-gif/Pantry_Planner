export function pickTags(name = "") {
const n = name.toLowerCase();
const map = [
{ match: ["pasta","spaghetti","penne","alfredo","arrabbiata","primavera"], tags: ["pasta","italian"] },
{ match: ["omelette","frittata","scrambled"], tags: ["omelette","breakfast"] },
{ match: ["toast","bruschetta","sandwich","panini","melt"], tags: ["sandwich","toast"] },
{ match: ["soup","ramen","broth","minestrone","egg drop"], tags: ["soup","bowl"] },
{ match: ["salad","caprese","slaw","panzanella"], tags: ["salad","healthy"] },
{ match: ["rice","pulao","pilaf","fried rice","paella","biryani"], tags: ["rice","bowl"] },
{ match: ["noodle","noodles","ramen"], tags: ["noodles"] },
{ match: ["wrap","quesadilla","tortilla","kathi","burrito","fajita","taco"], tags: ["mexican","wrap"] },
{ match: ["curry","masala","paneer","chana","dal","tikka"], tags: ["curry","indian"] },
{ match: ["stir fry","stir-fry","stir‑fry"], tags: ["stir fry","vegetable"] },
{ match: ["pizza"], tags: ["pizza"] },
{ match: ["pancake","parfait","smoothie","oat","oats"], tags: ["breakfast","oatmeal"] },
{ match: ["shrimp","prawn"], tags: ["shrimp","seafood"] },
{ match: ["chicken"], tags: ["chicken","grilled"] },
{ match: ["beef"], tags: ["beef"] },
{ match: ["mushroom"], tags: ["mushroom","food"] },
{ match: ["broccoli"], tags: ["broccoli","vegetable"] },
{ match: ["cauliflower","gobi"], tags: ["cauliflower","vegetable"] },
{ match: ["paneer"], tags: ["paneer","indian"] },
{ match: ["chickpea","chana"], tags: ["chickpea","healthy"] },
{ match: ["eggplant","aubergine"], tags: ["eggplant","mediterranean"] },
];
for (const r of map) if (r.match.some(k => n.includes(k))) return r.tags;
return ["home-cooked","food"]; // default
}

export function placeholder(name, w = 640, h = 400) {
const seed = encodeURIComponent((name || "recipe").toLowerCase());
return `https://picsum.photos/seed/${seed}/${w}/${h}`;
}

export function recipeImage(name, imageUrl, w = 640, h = 400) {
if (imageUrl && imageUrl.trim()) return imageUrl;
return placeholder(name, w, h);
}