package com.tbaar.eurder.common.exception;

import java.util.Map;

public abstract class NotFoundException extends EurderException {

    protected NotFoundException(ErrorCode code, Map<String, Object> params, String message) {
        super(code, params, message);
    }
}
