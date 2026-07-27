package com.main.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecipeRequest(
  @NotBlank @Size(max = 200) String name,
  String instructions,
  String imageUrl,
  Integer totalTimeMinutes
) {}
