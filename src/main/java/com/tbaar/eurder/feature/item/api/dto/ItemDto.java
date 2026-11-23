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
                      Map<ItemState, Long> availableUnits,
                      @Schema(types = {"string", "null"}, description = "Path of an uploaded image, or null for none", example = "images/3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90.png")
                      String image) {
}
