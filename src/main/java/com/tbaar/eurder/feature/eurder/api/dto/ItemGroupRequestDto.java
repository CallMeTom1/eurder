package com.tbaar.eurder.feature.eurder.api.dto;

import com.tbaar.eurder.feature.item.domain.ItemState;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ItemGroupRequestDto(
        @NotNull(message = "Item id is required")
        UUID itemId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than 0")
        Integer amount,

        ItemState state) {

    public ItemGroupRequestDto {
        state = state == null ? ItemState.PRISTINE : state;
    }

    public ItemGroupRequestDto(UUID itemId, Integer amount) {
        this(itemId, amount, ItemState.PRISTINE);
    }
}
