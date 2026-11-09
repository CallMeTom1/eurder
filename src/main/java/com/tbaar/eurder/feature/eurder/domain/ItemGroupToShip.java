package com.tbaar.eurder.feature.eurder.domain;

import com.tbaar.eurder.feature.customer.domain.Address;
import com.tbaar.eurder.feature.item.domain.ItemState;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.UUID;

public record ItemGroupToShip(UUID eurderId,
                              @Field("firstName") String customerFirstName,
                              @Field("lastName") String customerLastName,
                              Address address,
                              String itemName,
                              ItemState itemState,
                              int amount) {
}
