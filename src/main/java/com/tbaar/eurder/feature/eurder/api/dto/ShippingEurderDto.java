package com.tbaar.eurder.feature.eurder.api.dto;

import com.tbaar.eurder.feature.customer.api.dto.AddressDto;

import java.util.List;
import java.util.UUID;

public record ShippingEurderDto(UUID eurderId,
                                String customerName,
                                AddressDto address,
                                List<ShippingItemGroupDto> itemGroups) {
}
