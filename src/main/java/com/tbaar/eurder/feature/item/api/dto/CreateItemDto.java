package com.tbaar.eurder.feature.item.api.dto;

import com.tbaar.eurder.feature.item.domain.ItemState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record CreateItemDto(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        @Schema(description = "Name of the item; several items can share a name", example = "Chair")
        String name,

        @NotBlank(message = "Description is required")
        @Size(max = 1000, message = "Description must be at most 1000 characters")
        @Schema(description = "Description of the item, up to 1000 characters", example = "Wooden chair")
        String description,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 digits before and 2 after the decimal point")
        @Schema(description = "Base price in euro, greater than 0 and with at most 2 decimals; damaged units get a discount", example = "49.99")
        BigDecimal price,

        @NotNull(message = "Units are required")
        @Schema(description = "Number of units to create in each state (PRISTINE: full price, SLIGHTLY_DAMAGED: 5% discount, DAMAGED: 10% discount), from 0 to 1000", example = "{\"PRISTINE\": 5, \"DAMAGED\": 1}")
        Map<ItemState,
                @NotNull(message = "Units per state are required")
                @PositiveOrZero(message = "Units per state must be 0 or more")
                @Max(value = 1000, message = "Units per state must be at most 1000")
                Integer> units) {
}
