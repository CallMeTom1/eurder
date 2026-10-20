package com.tbaar.eurder.feature.item.api.dto;

import com.tbaar.eurder.feature.item.domain.ItemState;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record ItemDto(UUID id,
                      String name,
                      String description,
                      BigDecimal price,
                      Map<ItemState, Long> availableUnits) {
}
