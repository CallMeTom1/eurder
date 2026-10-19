package com.tbaar.eurder.feature.customer.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressDto(
        @NotBlank(message = "Street is required")
        @Size(max = 255, message = "Street must be at most 255 characters")
        String street,

        @NotBlank(message = "House number is required")
        @Size(max = 20, message = "House number must be at most 20 characters")
        String houseNumber,

        @NotBlank(message = "Postal code is required")
        @Size(max = 20, message = "Postal code must be at most 20 characters")
        String postalCode,

        @NotBlank(message = "City is required")
        @Size(max = 255, message = "City must be at most 255 characters")
        String city) {
}
