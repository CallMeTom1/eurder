package com.tbaar.eurder.feature.item.api.dto;

import com.tbaar.eurder.feature.item.domain.ItemState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record ItemDto(UUID id,
                      String name,
                      String description,
                      BigDecimal price,
                      @Schema(description = "Number of units still available in each state")
                      Map<ItemState, Long> availableUnits) {
}
