package com.tbaar.eurder.feature.security.exception;

import com.tbaar.eurder.common.exception.ConflictException;
import com.tbaar.eurder.common.exception.ErrorCode;

import java.util.Map;

public class EmailAlreadyUsedException extends ConflictException {

    public EmailAlreadyUsedException(String email) {
        super(ErrorCode.EMAIL_ALREADY_USED, Map.of("email", email), messageFor(email));
    }

    public EmailAlreadyUsedException(String email, Throwable cause) {
        super(ErrorCode.EMAIL_ALREADY_USED, Map.of("email", email), messageFor(email), cause);
    }

    private static String messageFor(String email) {
        return "Email " + email + " is already in use";
    }
}
