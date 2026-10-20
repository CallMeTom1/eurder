package com.tbaar.eurder.feature.item.api.dto;

import com.tbaar.eurder.feature.item.domain.ItemState;

import java.util.UUID;

public record ItemUnitDto(UUID id,
                          ItemState state,
                          boolean available) {
}
