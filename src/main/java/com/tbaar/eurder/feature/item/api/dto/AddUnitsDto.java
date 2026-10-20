package com.tbaar.eurder.feature.item.api.dto;

import com.tbaar.eurder.feature.item.domain.ItemState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddUnitsDto(
        @NotNull(message = "State is required")
        @Schema(description = "State of the units to add (PRISTINE: full price, SLIGHTLY_DAMAGED: 5% discount, DAMAGED: 10% discount)", example = "PRISTINE")
        ItemState state,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than 0")
        @Max(value = 1000, message = "Amount must be at most 1000")
        @Schema(description = "Number of units to add, from 1 to 1000", example = "10")
        Integer amount) {
}
