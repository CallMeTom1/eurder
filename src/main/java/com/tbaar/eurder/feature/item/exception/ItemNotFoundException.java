package com.tbaar.eurder.feature.item.exception;

import com.tbaar.eurder.common.exception.ErrorCode;
import com.tbaar.eurder.common.exception.NotFoundException;

import java.util.Map;
import java.util.UUID;

public class ItemNotFoundException extends NotFoundException {

    public ItemNotFoundException(UUID id) {
        super(ErrorCode.ITEM_NOT_FOUND, Map.of("id", id), "No item found for id " + id);
    }
}
