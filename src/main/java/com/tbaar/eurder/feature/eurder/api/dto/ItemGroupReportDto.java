package com.tbaar.eurder.feature.eurder.api.dto;

import com.tbaar.eurder.feature.item.domain.ItemState;

import java.math.BigDecimal;

public record ItemGroupReportDto(String itemName,
                                 ItemState itemState,
                                 int amount,
                                 BigDecimal subtotal) {
}
