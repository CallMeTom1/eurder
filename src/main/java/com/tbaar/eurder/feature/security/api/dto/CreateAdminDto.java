package com.tbaar.eurder.feature.security.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAdminDto(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must be at most 255 characters")
        @Schema(description = "Email, also used to log in; spaces and capitals are ignored", example = "jane@doe.be")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        @Schema(description = "Password used to log in, 8 to 72 characters", example = "secret123")
        String password
) {
}
