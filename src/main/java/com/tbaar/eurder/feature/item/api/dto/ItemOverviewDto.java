package com.tbaar.eurder.feature.item.api.dto;

import com.tbaar.eurder.feature.item.domain.ItemState;
import com.tbaar.eurder.feature.item.domain.StockUrgency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record ItemOverviewDto(UUID id,
                              String name,
                              String description,
                              BigDecimal price,
                              @Schema(description = "Number of units still available in each state")
                              Map<ItemState, Long> availableUnits,
                              @Schema(description = "Resupply urgency from the available units of all states: STOCK_LOW below 5, STOCK_MEDIUM below 10, STOCK_HIGH from 10")
                              StockUrgency urgency) {
}
