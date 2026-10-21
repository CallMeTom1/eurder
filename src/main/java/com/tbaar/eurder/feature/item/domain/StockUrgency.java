package com.tbaar.eurder.feature.item.domain;

import java.util.Arrays;

public enum StockUrgency {
    STOCK_LOW(0, 4),
    STOCK_MEDIUM(5, 9),
    STOCK_HIGH(10, Integer.MAX_VALUE);

    private final int minStock;
    private final int maxStock;

    StockUrgency(int minStock, int maxStock) {
        this.minStock = minStock;
        this.maxStock = maxStock;
    }

    public static StockUrgency from(int stock) {
        return Arrays.stream(values())
                .filter(urgency -> stock >= urgency.minStock && stock <= urgency.maxStock)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Stock must not be negative: " + stock));
    }

    public int getMinStock() {
        return minStock;
    }

    public int getMaxStock() {
        return maxStock;
    }
}
