package com.tbaar.eurder.feature.item.exception;

import com.tbaar.eurder.common.exception.NotFoundException;

import java.util.UUID;

public class ItemNotFoundException extends NotFoundException {

    public ItemNotFoundException(UUID id) {
        super("No item found for id " + id);
    }
}
