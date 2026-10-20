package com.tbaar.eurder.feature.eurder.exception;

import com.tbaar.eurder.common.exception.NotFoundException;

import java.util.UUID;

public class EurderNotFoundException extends NotFoundException {

    public EurderNotFoundException(UUID id) {
        super("No eurder found for id " + id);
    }
}
