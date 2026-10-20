package com.tbaar.eurder.feature.eurder.domain;

import com.tbaar.eurder.feature.customer.domain.Address;
import com.tbaar.eurder.feature.item.domain.ItemState;

import java.util.UUID;

public record ItemGroupToShip(UUID eurderId,
                              String customerFirstName,
                              String customerLastName,
                              Address address,
                              String itemName,
                              ItemState itemState,
                              int amount) {
}
