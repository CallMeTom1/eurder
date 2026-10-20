package com.tbaar.eurder.feature.item.api.dto;

import com.tbaar.eurder.feature.item.domain.ItemState;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddUnitsDto(
        @NotNull(message = "State is required")
        ItemState state,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than 0")
        @Max(value = 1000, message = "Amount must be at most 1000")
        Integer amount) {
}
