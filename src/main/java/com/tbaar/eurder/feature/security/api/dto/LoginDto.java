package com.tbaar.eurder.feature.security.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginDto(
        @Schema(example = "admin@eurder.be")
        @NotBlank(message = "Email is required")
        String email,

        @Schema(example = "admin")
        @NotBlank(message = "Password is required")
        String password) {
}
