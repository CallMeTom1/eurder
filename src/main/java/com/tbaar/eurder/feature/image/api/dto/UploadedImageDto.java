package com.tbaar.eurder.feature.image.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UploadedImageDto(
        @Schema(description = "Path of the image, to give to an item and to read under /api", example = "images/3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90.png")
        String path) {
}
