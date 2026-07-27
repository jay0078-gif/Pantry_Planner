package com.main.dto;

import java.math.BigDecimal;

public record PurchaseRequest(
BigDecimal price,
BigDecimal quantity,
String unit
) {}


