package com.tbaar.eurder.feature.item.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum ItemState {
    PRISTINE(0),
    SLIGHTLY_DAMAGED(5),
    DAMAGED(10);

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final int discountPercentage;

    ItemState(int discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public BigDecimal applyTo(BigDecimal basePrice) {
        return basePrice.multiply(BigDecimal.valueOf(100L - discountPercentage))
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
