package com.tbaar.eurder.feature.item.api.dto;

import com.tbaar.eurder.feature.item.domain.ItemState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ChangeUnitStateDto(
        @NotNull(message = "State is required")
        @Schema(description = "New state of the unit (PRISTINE: full price, SLIGHTLY_DAMAGED: 5% discount, DAMAGED: 10% discount)", example = "DAMAGED")
        ItemState state) {
}
