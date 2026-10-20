package com.tbaar.eurder.feature.customer.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCustomerDto(
        @NotBlank(message = "First name is required")
        @Size(max = 255, message = "First name must be at most 255 characters")
        @Schema(description = "First name of the customer", example = "Jane")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 255, message = "Last name must be at most 255 characters")
        @Schema(description = "Last name of the customer", example = "Doe")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must be at most 255 characters")
        @Schema(description = "Email, also used to log in; spaces and capitals are ignored", example = "jane@doe.be")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        @Schema(description = "Password used to log in, 8 to 72 characters", example = "secret123")
        String password,

        @NotNull(message = "Address is required")
        @Valid
        @Schema(description = "Address the eurders of the customer are shipped to")
        AddressDto address,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9 ./-]{6,20}$", message = "Phone number must be valid")
        @Schema(description = "Phone number: digits, spaces, dots, slashes or dashes, optionally starting with +", example = "+32 470 12 34 56")
        String phoneNumber) {
}
