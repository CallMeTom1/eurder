package com.tbaar.eurder.common.exception;

public enum ErrorCode {

    UNEXPECTED("error.common.unexpected"),
    BAD_REQUEST("error.common.bad-request"),
    VALIDATION_FAILED("error.common.validation-failed"),
    MALFORMED_BODY("error.common.malformed-body"),
    INVALID_VALUE("error.common.invalid-value"),
    INVALID_PARAMETER("error.common.invalid-parameter"),
    ROUTE_NOT_FOUND("error.common.route-not-found"),
    METHOD_NOT_ALLOWED("error.common.method-not-allowed"),
    NOT_ACCEPTABLE("error.common.not-acceptable"),
    UNSUPPORTED_MEDIA_TYPE("error.common.unsupported-media-type"),
    CONCURRENT_MODIFICATION("error.common.concurrent-modification"),

    AUTHENTICATION_REQUIRED("error.security.authentication-required"),
    INVALID_CREDENTIALS("error.security.invalid-credentials"),
    INVALID_ACCESS_TOKEN("error.security.invalid-access-token"),
    ACCESS_TOKEN_EXPIRED("error.security.access-token-expired"),
    INVALID_REFRESH_TOKEN("error.security.invalid-refresh-token"),
    FORBIDDEN("error.security.forbidden"),
    EMAIL_ALREADY_USED("error.security.email-already-used"),

    CUSTOMER_NOT_FOUND("error.customer.not-found"),

    EURDER_NOT_FOUND("error.eurder.not-found"),

    ITEM_NOT_FOUND("error.item.not-found"),
    ITEM_UNIT_NOT_FOUND("error.item.unit-not-found"),
    ITEM_UNIT_ALREADY_SOLD("error.item.unit-already-sold"),

    VALIDATION_REQUIRED("validation.required"),
    VALIDATION_TOO_SHORT("validation.too-short"),
    VALIDATION_TOO_LONG("validation.too-long"),
    VALIDATION_TOO_LARGE("validation.too-large"),
    VALIDATION_TOO_MANY_DIGITS("validation.too-many-digits"),
    VALIDATION_INVALID_FORMAT("validation.invalid-format"),
    VALIDATION_MUST_BE_POSITIVE("validation.must-be-positive"),
    VALIDATION_MUST_NOT_BE_NEGATIVE("validation.must-not-be-negative"),
    VALIDATION_INVALID("validation.invalid");

    private final String key;

    ErrorCode(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
