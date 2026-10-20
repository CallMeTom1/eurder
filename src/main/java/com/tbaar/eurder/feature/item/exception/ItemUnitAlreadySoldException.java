package com.tbaar.eurder.feature.item.exception;

import com.tbaar.eurder.common.exception.ConflictException;

import java.util.UUID;

public class ItemUnitAlreadySoldException extends ConflictException {

    public ItemUnitAlreadySoldException(UUID unitId) {
        super("Item unit " + unitId + " is already sold");
    }
}
