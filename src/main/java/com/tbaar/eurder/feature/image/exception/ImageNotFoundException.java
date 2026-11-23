package com.tbaar.eurder.feature.image.exception;

import com.tbaar.eurder.common.exception.ErrorCode;
import com.tbaar.eurder.common.exception.NotFoundException;

import java.util.Map;

public class ImageNotFoundException extends NotFoundException {

    public ImageNotFoundException(String name) {
        super(ErrorCode.IMAGE_NOT_FOUND, Map.of("name", name), "No image found for " + name);
    }
}
