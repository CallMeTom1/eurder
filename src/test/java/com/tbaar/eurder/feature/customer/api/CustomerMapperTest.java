package com.tbaar.eurder.feature.customer.api;

import com.tbaar.eurder.feature.customer.api.dto.AddressDto;
import com.tbaar.eurder.feature.customer.api.dto.CreateCustomerDto;
import com.tbaar.eurder.feature.customer.api.dto.CustomerDto;
import com.tbaar.eurder.feature.customer.domain.Address;
import com.tbaar.eurder.feature.customer.domain.Customer;
import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerMapperTest {

    private final CustomerMapper customerMapper = new CustomerMapper(new AddressMapper());

    @Test
    void givenCreateCustomerDtoAndItsCredentials_whenMappingToEntity_thenCopiesEveryField() {
        CreateCustomerDto dto = new CreateCustomerDto("Jane", "Doe", "jane@doe.be", "secret123",
                new AddressDto("Rue Haute", "1", "1000", "Bruxelles"), "0470123456");
        Credentials credentials = new Credentials("jane@doe.be", "hash", Role.CUSTOMER);

        Customer customer = customerMapper.toEntity(dto, credentials);

        assertThat(customer.getCredentials()).isSameAs(credentials);
        assertThat(customer.getFirstName()).isEqualTo("Jane");
        assertThat(customer.getLastName()).isEqualTo("Doe");
        assertThat(customer.getPhoneNumber()).isEqualTo("0470123456");
        assertThat(customer.getAddress())
                .extracting(Address::getStreet, Address::getHouseNumber, Address::getPostalCode, Address::getCity)
                .containsExactly("Rue Haute", "1", "1000", "Bruxelles");
    }

    @Test
    void givenCustomer_whenMappingToDto_thenExposesItsCredentialsEmailButNoPassword() {
        Customer customer = new Customer(new Credentials("jane@doe.be", "hash", Role.CUSTOMER),
                "Jane", "Doe", new Address("Rue Haute", "1", "1000", "Bruxelles"), "0470123456");

        CustomerDto dto = customerMapper.toDto(customer);

        assertThat(dto).isEqualTo(new CustomerDto(customer.getId(), "Jane", "Doe", "jane@doe.be",
                new AddressDto("Rue Haute", "1", "1000", "Bruxelles"), "0470123456"));
    }
}
