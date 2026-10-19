package com.tbaar.eurder.feature.customer.domain;

import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.CredentialsRepository;
import com.tbaar.eurder.feature.security.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CustomerRepositoryIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Test
    void givenCustomerWithUnsavedCredentials_whenSaving_thenTheCredentialsAreSavedToo() {
        Customer jane = aCustomer(new Credentials("jane@doe.be", "hash", Role.CUSTOMER));

        customerRepository.saveAndFlush(jane);

        assertThat(credentialsRepository.findByEmail("jane@doe.be")).isPresent();
    }

    @Test
    void givenSavedCustomer_whenFindingByItsCredentialsId_thenReturnsTheCustomerWithItsAddress() {
        Customer saved = customerRepository.saveAndFlush(aCustomer(new Credentials("jane@doe.be", "hash", Role.CUSTOMER)));

        Optional<Customer> found = customerRepository.findByCredentialsId(saved.getCredentials().getId());

        assertThat(found).hasValueSatisfying(customer -> {
            assertThat(customer.getId()).isEqualTo(saved.getId());
            assertThat(customer.getFirstName()).isEqualTo("Jane");
            assertThat(customer.getAddress().getCity()).isEqualTo("Bruxelles");
            assertThat(customer.getPhoneNumber()).isEqualTo("0470123456");
        });
    }

    private static Customer aCustomer(Credentials credentials) {
        return new Customer(credentials, "Jane", "Doe", new Address("Rue Haute", "1", "1000", "Bruxelles"), "0470123456");
    }
}
