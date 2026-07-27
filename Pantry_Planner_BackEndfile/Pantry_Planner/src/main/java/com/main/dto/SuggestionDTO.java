package com.main.dto;

public record SuggestionDTO(
Long recipeId,
String name,
String imageUrl,
int totalIngredients,
int matched,
int missing,
java.util.List<String> missingIngredients
) {}
