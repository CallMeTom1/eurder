package com.tbaar.eurder.feature.eurder.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record EurderDto(UUID id,
                        List<ItemGroupDto> itemGroups,
                        BigDecimal totalPrice) {
}
