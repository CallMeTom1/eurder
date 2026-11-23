package com.tbaar.eurder.feature.item.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateItemDto(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        @Schema(description = "Name of the item; several items can share a name", example = "Chair")
        String name,

        @NotBlank(message = "Description is required")
        @Size(max = 255, message = "Description must be at most 255 characters")
        @Schema(description = "Description of the item, up to 255 characters", example = "Wooden chair")
        String description,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 digits before and 2 after the decimal point")
        @Schema(description = "Base price in euro, greater than 0 and with at most 2 decimals; damaged units get a discount", example = "49.99")
        BigDecimal price,

        @Pattern(regexp = "images/[0-9a-f-]{36}\\.(png|jpg|webp)", message = "Image must be the path of an uploaded image")
        @Schema(types = {"string", "null"}, description = "Path of an uploaded image, or null for none", example = "images/3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90.png")
        String image) {
}
