package com.tbaar.eurder.feature.item.api.dto;

import com.tbaar.eurder.feature.item.domain.ItemState;
import jakarta.validation.constraints.NotNull;

public record ChangeUnitStateDto(
        @NotNull(message = "State is required")
        ItemState state) {
}
