package com.tbaar.eurder.feature.eurder.api.dto;

import com.tbaar.eurder.feature.item.domain.ItemState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ItemGroupRequestDto(
        @NotNull(message = "Item id is required")
        @Schema(description = "Id of the item to order", example = "3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90")
        UUID itemId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than 0")
        @Schema(description = "Number of units to order; ships the next day if enough units of that state are available, otherwise a week later", example = "2")
        Integer amount,

        @Schema(description = "State of the units to order, PRISTINE when left out (PRISTINE: full price, SLIGHTLY_DAMAGED: 5% discount, DAMAGED: 10% discount)", example = "PRISTINE")
        ItemState state) {

    public ItemGroupRequestDto {
        state = state == null ? ItemState.PRISTINE : state;
    }

    public ItemGroupRequestDto(UUID itemId, Integer amount) {
        this(itemId, amount, ItemState.PRISTINE);
    }
}
