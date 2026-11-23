package com.tbaar.eurder.feature.image.exception;

import com.tbaar.eurder.common.exception.BadRequestException;
import com.tbaar.eurder.common.exception.ErrorCode;

import java.util.Map;

public class InvalidImageException extends BadRequestException {

    public InvalidImageException() {
        super(ErrorCode.IMAGE_INVALID_TYPE, Map.of("types", "PNG, JPEG, WebP"), "The file is not a PNG, JPEG or WebP image");
    }
}
