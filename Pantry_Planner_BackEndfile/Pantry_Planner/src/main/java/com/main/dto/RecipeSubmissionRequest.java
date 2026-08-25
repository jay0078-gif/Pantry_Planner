package com.main.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RecipeSubmissionRequest(
        @NotBlank
        @Size(max = 200)
        String title,
        @NotBlank
        @Size(max = 5000)
        String instructions,
        @Valid
        @Size(min = 1, max = 50)
        List<@NotBlank @Size(max = 100) String> ingredients) {
}
