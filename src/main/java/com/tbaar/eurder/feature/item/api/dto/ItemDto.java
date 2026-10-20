package com.tbaar.eurder.feature.item.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemDto(UUID id,
                      String name,
                      String description,
                      BigDecimal price,
                      int stock) {
}
