package com.tbaar.eurder.common.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base of the business exceptions. Its subclass decides the HTTP status, its {@link ErrorCode} the translated message.
 * The message stays in English, for the logs and the {@code detail} of the response.
 */
public abstract class EurderException extends RuntimeException implements HasErrorCode {

    private final ErrorCode code;
    private final Map<String, Object> params;

    protected EurderException(ErrorCode code, Map<String, Object> params, String message) {
        super(message);
        this.code = code;
        this.params = copyOf(params);
    }

    protected EurderException(ErrorCode code, Map<String, Object> params, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.params = copyOf(params);
    }

    // Unlike Map.copyOf, keeps a null value instead of hiding the business error behind a NullPointerException.
    private static Map<String, Object> copyOf(Map<String, Object> params) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }

    @Override
    public ErrorCode code() {
        return code;
    }

    @Override
    public Map<String, Object> params() {
        return params;
    }
}
