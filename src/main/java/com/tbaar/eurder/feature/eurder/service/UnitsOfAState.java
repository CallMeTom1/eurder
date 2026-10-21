package com.tbaar.eurder.feature.eurder.service;

import com.tbaar.eurder.feature.eurder.api.dto.ItemGroupRequestDto;
import com.tbaar.eurder.feature.item.domain.ItemState;

import java.util.UUID;

record UnitsOfAState(UUID itemId, ItemState state) {

    static UnitsOfAState from(ItemGroupRequestDto requested) {
        return new UnitsOfAState(requested.itemId(), requested.state());
    }
}
