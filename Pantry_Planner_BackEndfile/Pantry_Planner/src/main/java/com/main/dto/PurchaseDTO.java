package com.main.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PurchaseDTO(
Long id,
Long ingredientId,
String ingredientName,
BigDecimal quantity,
String unit,
BigDecimal price,
Instant purchasedAt
) {}