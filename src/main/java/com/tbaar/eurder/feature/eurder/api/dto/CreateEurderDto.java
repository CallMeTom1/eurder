package com.tbaar.eurder.feature.eurder.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateEurderDto(
        @NotEmpty(message = "At least one item group is required")
        @Schema(description = "Lines of the eurder, handled in the given order: the same item can appear twice")
        List<@NotNull(message = "Item group is required") @Valid ItemGroupRequestDto> itemGroups) {
}
