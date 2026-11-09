package com.tbaar.eurder.feature.security.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokensDto(
        @Schema(description = "JWT to send as Authorization: Bearer <token>")
        String accessToken,

        @Schema(example = "Bearer")
        String tokenType,

        @Schema(description = "Lifetime of the access token, in seconds", example = "900")
        long expiresIn) {
}
