package com.main.dto;

public record SuggestionDTO(
Long recipeId,
String name,
String imageUrl,
String imageSourceUrl,
String imagePhotographer,
String imagePhotographerUrl,
int totalIngredients,
int matched,
int missing,
java.util.List<String> missingIngredients
) {}
