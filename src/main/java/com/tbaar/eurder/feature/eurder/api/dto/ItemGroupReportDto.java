package com.tbaar.eurder.feature.eurder.api.dto;

import java.math.BigDecimal;

public record ItemGroupReportDto(String itemName,
                                 int amount,
                                 BigDecimal subtotal) {
}
