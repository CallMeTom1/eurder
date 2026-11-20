package com.tbaar.eurder.feature.security.api.dto;

import com.tbaar.eurder.feature.security.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record CurrentUserDto(
        @Schema(description = "Id of the credentials, the subject of the access token")
        UUID id,

        @Schema(example = "CUSTOMER")
        Role role,

        @Schema(example = "jane@doe.be")
        String email,

        @Schema(types = {"string", "null"}, description = "Null for an admin, who has no customer profile", example = "Jane")
        String firstName,

        @Schema(types = {"string", "null"}, description = "Null for an admin, who has no customer profile", example = "Doe")
        String lastName) {
}
