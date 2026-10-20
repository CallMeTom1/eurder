package com.tbaar.eurder.feature.item.exception;

import com.tbaar.eurder.common.exception.NotFoundException;

import java.util.UUID;

public class ItemUnitNotFoundException extends NotFoundException {

    public ItemUnitNotFoundException(UUID itemId, UUID unitId) {
        super("No unit " + unitId + " found for item " + itemId);
    }
}
