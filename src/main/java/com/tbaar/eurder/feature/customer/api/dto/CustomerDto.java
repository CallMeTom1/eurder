package com.tbaar.eurder.feature.customer.api.dto;

import java.util.UUID;

public record CustomerDto(UUID id,
                          String firstName,
                          String lastName,
                          String email,
                          AddressDto address,
                          String phoneNumber) {
}
