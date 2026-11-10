package com.tbaar.eurder.common.exception;

import java.util.Map;

/**
 * An exception that tells the front end what went wrong: a translatable {@link ErrorCode} and the values to insert in it.
 * Implemented apart from {@link EurderException} by the exceptions that must extend a Spring class.
 */
public interface HasErrorCode {

    ErrorCode code();

    Map<String, Object> params();
}
