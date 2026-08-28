import assert from "node:assert/strict";
import { existsSync } from "node:fs";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import test from "node:test";

import {
  isUnusableRecipeImage,
  recipeCover,
  recipeCoverKey,
} from "./recipeCovers.js";

const recipesUrl = new URL(
  "../../../../Pantry_Planner_BackEndfile/Pantry_Planner/src/main/resources/data/recipes.json",
  import.meta.url
);

test("every seeded recipe maps to an existing local cover", async () => {
  const recipes = JSON.parse(await readFile(recipesUrl, "utf8"));

  assert.equal(recipes.length, 250);
  for (const recipe of recipes) {
    const coverUrl = recipeCover(recipe.name);
    assert.equal(new URL(coverUrl).protocol, "file:");
    assert.ok(existsSync(fileURLToPath(coverUrl)), recipe.name);
  }
});

test("visible recipes map to relevant covers", () => {
  assert.equal(recipeCoverKey("Tomato Pasta"), "pasta");
  assert.equal(recipeCoverKey("Veg Omelette"), "breakfast");
  assert.equal(recipeCoverKey("Peanut Butter Toast"), "sandwich");
  assert.equal(recipeCoverKey("Chana Masala"), "curry");
  assert.equal(recipeCoverKey("Fried Rice"), "rice");
});

test("missing and known broken remote images use local covers", () => {
  assert.equal(isUnusableRecipeImage(null), true);
  assert.equal(isUnusableRecipeImage(""), true);
  assert.equal(
    isUnusableRecipeImage("https://source.unsplash.com/640x400/?pasta"),
    true
  );
  assert.equal(
    isUnusableRecipeImage(
      "https://images.pexels.com/photos/.../pexels-photo.jpeg"
    ),
    true
  );
  assert.equal(
    isUnusableRecipeImage(
      "https://images.pexels.com/photos/410648/pexels-photo-410648.jpeg"
    ),
    false
  );
});
