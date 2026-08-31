package com.main.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PantryUpsertRequest(
        @Size(max = 120) String ingredientName,
        @DecimalMin("0.0") BigDecimal quantity,
        @Size(max = 50) String unit
) {}
