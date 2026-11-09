package com.tbaar.eurder.feature.eurder.domain;

import com.tbaar.eurder.feature.customer.domain.Address;
import com.tbaar.eurder.feature.customer.domain.Customer;

import java.util.UUID;

public record EurderCustomer(UUID id,
                             UUID credentialsId,
                             String firstName,
                             String lastName,
                             Address address) {

    static EurderCustomer from(Customer customer) {
        return new EurderCustomer(customer.getId(), customer.getCredentialsId(), customer.getFirstName(),
                customer.getLastName(), customer.getAddress());
    }
}
