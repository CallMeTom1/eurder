package com.tbaar.eurder.feature.item.domain;

import java.util.UUID;

public record AvailableUnits(UUID itemId,
                             ItemState state,
                             long count) {
}
