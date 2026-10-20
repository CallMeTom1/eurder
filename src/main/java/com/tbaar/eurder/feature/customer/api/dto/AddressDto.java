package com.tbaar.eurder.feature.customer.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressDto(
        @NotBlank(message = "Street is required")
        @Size(max = 255, message = "Street must be at most 255 characters")
        @Schema(description = "Street", example = "Rue Haute")
        String street,

        @NotBlank(message = "House number is required")
        @Size(max = 20, message = "House number must be at most 20 characters")
        @Schema(description = "House number", example = "1")
        String houseNumber,

        @NotBlank(message = "Postal code is required")
        @Size(max = 20, message = "Postal code must be at most 20 characters")
        @Schema(description = "Postal code", example = "1000")
        String postalCode,

        @NotBlank(message = "City is required")
        @Size(max = 255, message = "City must be at most 255 characters")
        @Schema(description = "City", example = "Bruxelles")
        String city) {
}
