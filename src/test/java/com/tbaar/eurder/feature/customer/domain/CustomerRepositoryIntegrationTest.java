package com.tbaar.eurder.feature.customer.domain;

import com.tbaar.eurder.common.UniqueEmail;
import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.CredentialsRepository;
import com.tbaar.eurder.feature.security.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
class CustomerRepositoryIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    private final String janeEmail = UniqueEmail.of("jane");

    @Test
    void givenCustomerWithUnsavedCredentials_whenSaving_thenTheCredentialsAreSavedToo() {
        Customer jane = aCustomer(new Credentials(janeEmail, "hash", Role.CUSTOMER));

        customerRepository.saveAndFlush(jane);

        assertThat(credentialsRepository.findByEmail(janeEmail)).isPresent();
    }

    @Test
    void givenSavedCustomer_whenFindingByItsCredentialsId_thenReturnsTheCustomerWithItsAddress() {
        Customer saved = customerRepository.saveAndFlush(aCustomer(new Credentials(janeEmail, "hash", Role.CUSTOMER)));

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
        String zoeEmail = UniqueEmail.of("zoe");
        String aliceEmail = UniqueEmail.of("alice");
        customerRepository.saveAndFlush(aCustomer(janeEmail, "Jane", "Doe"));
        customerRepository.saveAndFlush(aCustomer(zoeEmail, "Zoe", "Adams"));
        customerRepository.saveAndFlush(aCustomer(aliceEmail, "Alice", "Doe"));

        List<Customer> customers = customerRepository.findAllByOrderByLastNameAscFirstNameAsc();

        assertThat(customers)
                .filteredOn(customer -> Set.of(janeEmail, zoeEmail, aliceEmail).contains(customer.getCredentials().getEmail()))
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
