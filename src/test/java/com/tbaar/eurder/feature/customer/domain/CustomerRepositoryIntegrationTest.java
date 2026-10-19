package com.tbaar.eurder.feature.customer.domain;

import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.CredentialsRepository;
import com.tbaar.eurder.feature.security.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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

    @Test
    void givenCustomersSavedInAnyOrder_whenFindingAllSorted_thenReturnsThemByLastNameThenFirstName() {
        customerRepository.saveAndFlush(aCustomer("jane@doe.be", "Jane", "Doe"));
        customerRepository.saveAndFlush(aCustomer("zoe@adams.be", "Zoe", "Adams"));
        customerRepository.saveAndFlush(aCustomer("alice@doe.be", "Alice", "Doe"));

        List<Customer> customers = customerRepository.findAllByOrderByLastNameAscFirstNameAsc();

        assertThat(customers)
                .extracting(Customer::getFirstName, Customer::getLastName)
                .containsExactly(tuple("Zoe", "Adams"), tuple("Alice", "Doe"), tuple("Jane", "Doe"));
    }

    private static Customer aCustomer(Credentials credentials) {
        return new Customer(credentials, "Jane", "Doe", new Address("Rue Haute", "1", "1000", "Bruxelles"), "0470123456");
    }

    private static Customer aCustomer(String email, String firstName, String lastName) {
        return new Customer(new Credentials(email, "hash", Role.CUSTOMER), firstName, lastName,
                new Address("Rue Haute", "1", "1000", "Bruxelles"), "0470123456");
    }
}
