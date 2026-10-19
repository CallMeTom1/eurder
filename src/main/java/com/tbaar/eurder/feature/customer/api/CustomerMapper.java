package com.tbaar.eurder.feature.customer.api;

import com.tbaar.eurder.feature.customer.api.dto.CreateCustomerDto;
import com.tbaar.eurder.feature.customer.api.dto.CustomerDto;
import com.tbaar.eurder.feature.customer.domain.Customer;
import com.tbaar.eurder.feature.security.domain.Credentials;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    private final AddressMapper addressMapper;

    public CustomerMapper(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    public Customer toEntity(CreateCustomerDto dto, Credentials credentials) {
        return new Customer(credentials, dto.firstName(), dto.lastName(), addressMapper.toEntity(dto.address()), dto.phoneNumber());
    }

    public CustomerDto toDto(Customer customer) {
        return new CustomerDto(customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getCredentials().getEmail(),
                addressMapper.toDto(customer.getAddress()),
                customer.getPhoneNumber());
    }
}
