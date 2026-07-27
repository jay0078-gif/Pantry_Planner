package com.main.dto;

import java.math.BigDecimal;

public record PantryUpsertRequest(
String ingredientName,
BigDecimal quantity,
String unit
) {}
