package com.tbaar.eurder.feature.eurder.domain;

import com.tbaar.eurder.feature.customer.domain.Address;

import java.util.UUID;

public record ItemGroupToShip(UUID eurderId,
                              String customerFirstName,
                              String customerLastName,
                              Address address,
                              String itemName,
                              int amount) {
}
