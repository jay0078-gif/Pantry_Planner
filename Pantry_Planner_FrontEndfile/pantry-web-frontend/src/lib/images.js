import { backendBaseUrl } from "../api";
import {
  isUnusableRecipeImage,
  recipeCover,
} from "./recipeCovers";

export function publicAsset(path) {
  return `${import.meta.env.BASE_URL}${path.replace(/^\/+/, "")}`;
}

export function resolveImageUrl(
  imageUrl,
  fallbackUrl = publicAsset("images/food-fallback.svg")
) {
  const value = imageUrl?.trim();

  if (!value) return fallbackUrl;

  if (/^(?:https?:|data:|blob:)/i.test(value)) return value;
  if (!backendBaseUrl) return fallbackUrl;

  return `${backendBaseUrl}/${value.replace(/^\/+/, "")}`;
}

export function recipeImage(name, imageUrl) {
  const cover = recipeCover(name);
  if (isUnusableRecipeImage(imageUrl)) return cover;
  return resolveImageUrl(imageUrl, cover);
}

export { recipeCover } from "./recipeCovers";
