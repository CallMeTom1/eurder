package com.tbaar.eurder.feature.item.exception;

import com.tbaar.eurder.common.exception.ErrorCode;
import com.tbaar.eurder.common.exception.NotFoundException;

import java.util.Map;
import java.util.UUID;

public class ItemUnitNotFoundException extends NotFoundException {

    public ItemUnitNotFoundException(UUID itemId, UUID unitId) {
        super(ErrorCode.ITEM_UNIT_NOT_FOUND, Map.of("itemId", itemId, "unitId", unitId),
                "No unit " + unitId + " found for item " + itemId);
    }
}
