package com.tbaar.eurder.feature.item.api.dto;

import com.tbaar.eurder.feature.item.domain.StockUrgency;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemOverviewDto(UUID id,
                              String name,
                              String description,
                              BigDecimal price,
                              int stock,
                              StockUrgency urgency) {
}
