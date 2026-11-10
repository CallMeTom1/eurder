package com.tbaar.eurder.common.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class EurderException extends RuntimeException implements HasErrorCode {

    private final ErrorCode code;
    private final Map<String, Object> params;

    protected EurderException(ErrorCode code, Map<String, Object> params, String message) {
        super(message);
        this.code = code;
        this.params = copyKeepingNullValues(params);
    }

    protected EurderException(ErrorCode code, Map<String, Object> params, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.params = copyKeepingNullValues(params);
    }

    private static Map<String, Object> copyKeepingNullValues(Map<String, Object> params) {
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
