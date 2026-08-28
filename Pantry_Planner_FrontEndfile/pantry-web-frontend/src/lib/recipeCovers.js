const coverImages = {
  baked: new URL("../assets/recipe-covers/baked.jpg", import.meta.url).href,
  breakfast: new URL("../assets/recipe-covers/breakfast.jpg", import.meta.url).href,
  curry: new URL("../assets/recipe-covers/curry.jpg", import.meta.url).href,
  dessert: new URL("../assets/recipe-covers/dessert.jpg", import.meta.url).href,
  general: new URL("../assets/recipe-covers/general.jpg", import.meta.url).href,
  pasta: new URL("../assets/recipe-covers/pasta.jpg", import.meta.url).href,
  protein: new URL("../assets/recipe-covers/protein.jpg", import.meta.url).href,
  rice: new URL("../assets/recipe-covers/rice.jpg", import.meta.url).href,
  salad: new URL("../assets/recipe-covers/salad.jpg", import.meta.url).href,
  sandwich: new URL("../assets/recipe-covers/sandwich.jpg", import.meta.url).href,
  soup: new URL("../assets/recipe-covers/soup.jpg", import.meta.url).href,
};

const coverRules = [
  {
    cover: "breakfast",
    terms: [
      "omelette",
      "omelet",
      "frittata",
      "scrambled",
      "pancake",
      "breakfast",
      "oatmeal",
      "oats",
      "parfait",
      "smoothie",
      "french toast",
    ],
  },
  {
    cover: "dessert",
    terms: [
      "cake",
      "cookie",
      "brownie",
      "chocolate",
      "pudding",
      "dessert",
      "fruit",
      "banana",
      "muffin",
      "sweet",
    ],
  },
  {
    cover: "pasta",
    terms: [
      "pasta",
      "spaghetti",
      "penne",
      "lasagna",
      "mac and cheese",
      "ravioli",
      "gnocchi",
      "alfredo",
    ],
  },
  {
    cover: "sandwich",
    terms: [
      "sandwich",
      "toast",
      "wrap",
      "quesadilla",
      "tortilla",
      "burrito",
      "taco",
      "burger",
      "panini",
      "bruschetta",
    ],
  },
  {
    cover: "curry",
    terms: [
      "curry",
      "masala",
      "paneer",
      "chana",
      "dal",
      "tikka",
      "korma",
      "saag",
    ],
  },
  {
    cover: "rice",
    terms: [
      "rice",
      "noodle",
      "ramen",
      "stir fry",
      "stir-fry",
      "stir‑fry",
      "pulao",
      "pilaf",
      "paella",
      "biryani",
    ],
  },
  {
    cover: "soup",
    terms: ["soup", "broth", "stew", "chowder", "chili"],
  },
  {
    cover: "salad",
    terms: [
      "salad",
      "slaw",
      "hummus",
      "guacamole",
      "salsa",
      "vegetable",
      "veggie",
      "broccoli",
      "cauliflower",
      "eggplant",
      "aubergine",
    ],
  },
  {
    cover: "baked",
    terms: [
      "pizza",
      "garlic bread",
      "potato",
      "wedges",
      "fries",
      "roasted",
      "baked",
      "gratin",
    ],
  },
  {
    cover: "protein",
    terms: [
      "chicken",
      "beef",
      "shrimp",
      "prawn",
      "tuna",
      "fish",
      "salmon",
      "pork",
      "meatball",
    ],
  },
];

const unusableImagePatterns = [
  /^https?:\/\/source\.unsplash\.com\//i,
  /^https?:\/\/picsum\.photos\//i,
  /^https?:\/\/images\.pexels\.com\/photos\/\.{3}\//i,
  /(?:^|\/)images\/food-fallback(?:-wide)?\.[a-z0-9]+$/i,
];

export function recipeCoverKey(name = "") {
  const normalizedName = String(name || "").trim().toLowerCase();
  const rule = coverRules.find(({ terms }) =>
    terms.some((term) => normalizedName.includes(term))
  );

  return rule?.cover || "general";
}

export function recipeCover(name = "") {
  return coverImages[recipeCoverKey(name)];
}

export function isUnusableRecipeImage(imageUrl) {
  const value = typeof imageUrl === "string" ? imageUrl.trim() : "";
  return !value || unusableImagePatterns.some((pattern) => pattern.test(value));
}
