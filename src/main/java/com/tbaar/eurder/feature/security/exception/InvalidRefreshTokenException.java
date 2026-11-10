package com.tbaar.eurder.feature.security.exception;

import com.tbaar.eurder.common.exception.ErrorCode;
import com.tbaar.eurder.common.exception.HasErrorCode;
import org.springframework.security.core.AuthenticationException;

import java.util.Map;

public class InvalidRefreshTokenException extends AuthenticationException implements HasErrorCode {

    public InvalidRefreshTokenException() {
        super("Invalid refresh token");
    }

    @Override
    public ErrorCode code() {
        return ErrorCode.INVALID_REFRESH_TOKEN;
    }

    @Override
    public Map<String, Object> params() {
        return Map.of();
    }
}
