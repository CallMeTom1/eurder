package com.tbaar.eurder.feature.image.service;

import org.springframework.http.MediaType;

public record StoredImage(byte[] content, MediaType mediaType) {
}
