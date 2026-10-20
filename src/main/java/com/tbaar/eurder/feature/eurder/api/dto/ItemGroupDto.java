package com.tbaar.eurder.feature.eurder.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ItemGroupDto(UUID itemId,
                           String itemName,
                           BigDecimal itemPrice,
                           int amount,
                           BigDecimal subtotal,
                           LocalDate shippingDate) {
}
