package com.tbaar.eurder.feature.customer.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCustomerDto(
        @NotBlank(message = "First name is required")
        @Size(max = 255, message = "First name must be at most 255 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 255, message = "Last name must be at most 255 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password,

        @NotNull(message = "Address is required")
        @Valid
        AddressDto address,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9 ./-]{6,20}$", message = "Phone number must be valid")
        String phoneNumber) {
}
