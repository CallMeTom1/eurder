package com.tbaar.eurder.feature.eurder.api.dto;

import com.tbaar.eurder.feature.item.domain.ItemState;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ItemGroupDto(UUID itemId,
                           String itemName,
                           ItemState itemState,
                           BigDecimal itemPrice,
                           int amount,
                           BigDecimal subtotal,
                           LocalDate shippingDate) {
}
