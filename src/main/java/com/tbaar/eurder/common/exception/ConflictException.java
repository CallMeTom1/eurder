package com.tbaar.eurder.common.exception;

import java.util.Map;

public abstract class ConflictException extends EurderException {

    protected ConflictException(ErrorCode code, Map<String, Object> params, String message) {
        super(code, params, message);
    }

    protected ConflictException(ErrorCode code, Map<String, Object> params, String message, Throwable cause) {
        super(code, params, message, cause);
    }
}
