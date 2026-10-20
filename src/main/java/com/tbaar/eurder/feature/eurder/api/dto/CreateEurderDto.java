package com.tbaar.eurder.feature.eurder.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateEurderDto(
        @NotEmpty(message = "At least one item group is required")
        List<@NotNull(message = "Item group is required") @Valid ItemGroupRequestDto> itemGroups) {
}
