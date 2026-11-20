package com.tbaar.eurder.feature.item.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogItemDto(UUID id,
                             @Schema(example = "Chair")
                             String name,
                             @Schema(example = "Wooden chair")
                             String description,
                             @Schema(example = "49.99")
                             BigDecimal price) {
}
