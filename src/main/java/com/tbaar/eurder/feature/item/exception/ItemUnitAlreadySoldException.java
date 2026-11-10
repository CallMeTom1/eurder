package com.tbaar.eurder.feature.item.exception;

import com.tbaar.eurder.common.exception.ConflictException;
import com.tbaar.eurder.common.exception.ErrorCode;

import java.util.Collections;
import java.util.UUID;

public class ItemUnitAlreadySoldException extends ConflictException {

    public ItemUnitAlreadySoldException(UUID unitId) {
        // singletonMap accepts the null id of a unit that is not saved yet
        super(ErrorCode.ITEM_UNIT_ALREADY_SOLD, Collections.singletonMap("unitId", unitId), "Item unit " + unitId + " is already sold");
    }
}
