package com.tbaar.eurder.common.exception;

import java.util.Map;

public abstract class BadRequestException extends EurderException {

    protected BadRequestException(ErrorCode code, Map<String, Object> params, String message) {
        super(code, params, message);
    }
}
