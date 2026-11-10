package com.tbaar.eurder.feature.eurder.exception;

import com.tbaar.eurder.common.exception.ErrorCode;
import com.tbaar.eurder.common.exception.NotFoundException;

import java.util.Map;
import java.util.UUID;

public class EurderNotFoundException extends NotFoundException {

    public EurderNotFoundException(UUID id) {
        super(ErrorCode.EURDER_NOT_FOUND, Map.of("id", id), "No eurder found for id " + id);
    }
}
