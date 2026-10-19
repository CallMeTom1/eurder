package com.tbaar.eurder.feature.customer.api;

import com.tbaar.eurder.feature.customer.api.dto.AddressDto;
import com.tbaar.eurder.feature.customer.domain.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public Address toEntity(AddressDto dto) {
        return new Address(dto.street(), dto.houseNumber(), dto.postalCode(), dto.city());
    }

    public AddressDto toDto(Address address) {
        return new AddressDto(address.getStreet(), address.getHouseNumber(), address.getPostalCode(), address.getCity());
    }
}
