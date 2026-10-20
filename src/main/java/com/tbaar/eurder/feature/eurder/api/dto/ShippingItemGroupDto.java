package com.tbaar.eurder.feature.eurder.api.dto;

import com.tbaar.eurder.feature.item.domain.ItemState;

public record ShippingItemGroupDto(String itemName,
                                   ItemState itemState,
                                   int amount) {
}
