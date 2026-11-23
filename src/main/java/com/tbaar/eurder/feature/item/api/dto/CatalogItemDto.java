package com.tbaar.eurder.feature.item.api.dto;

import com.tbaar.eurder.feature.item.domain.StockUrgency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogItemDto(UUID id,
                             @Schema(example = "Chair")
                             String name,
                             @Schema(example = "Wooden chair")
                             String description,
                             @Schema(example = "49.99")
                             BigDecimal price,
                             @Schema(description = "Number of units available to order, in any state", example = "12")
                             long stock,
                             @Schema(description = "Stock urgency from the available units: STOCK_LOW below 5, STOCK_MEDIUM below 10, STOCK_HIGH from 10")
                             StockUrgency urgency) {
}
