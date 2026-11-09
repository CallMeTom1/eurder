package com.tbaar.eurder.feature.customer.domain;

import com.tbaar.eurder.common.UniqueEmail;
import com.tbaar.eurder.common.config.MongoConfig;
import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;

@DataMongoTest
@Import(MongoConfig.class)
class CustomerRepositoryIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    private final String janeEmail = UniqueEmail.of("jane");

    @Test
    void givenSavedCustomer_whenFindingByItsCredentialsId_thenReturnsTheCustomerWithItsAddress() {
        Customer saved = customerRepository.save(aCustomer(new Credentials(janeEmail, "hash", Role.CUSTOMER)));

        Optional<Customer> found = customerRepository.findByCredentialsId(saved.getCredentialsId());

        assertThat(found).hasValueSatisfying(customer -> {
            assertThat(customer.getId()).isEqualTo(saved.getId());
            assertThat(customer.getEmail()).isEqualTo(janeEmail);
            assertThat(customer.getFirstName()).isEqualTo("Jane");
            assertThat(customer.getAddress().getCity()).isEqualTo("Bruxelles");
            assertThat(customer.getPhoneNumber()).isEqualTo("0470123456");
        });
    }

    @Test
    void givenACustomerOfTheseCredentials_whenSavingAnotherOne_thenTheUniqueIndexRefusesIt() {
        Credentials credentials = new Credentials(janeEmail, "hash", Role.CUSTOMER);
        customerRepository.save(aCustomer(credentials));

        Throwable thrown = catchThrowable(() -> customerRepository.save(aCustomer(credentials)));

        assertThat(thrown).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void givenCustomersSavedInAnyOrder_whenFindingAllSorted_thenReturnsThemByLastNameThenFirstName() {
        String zoeEmail = UniqueEmail.of("zoe");
        String aliceEmail = UniqueEmail.of("alice");
        customerRepository.save(aCustomer(janeEmail, "Jane", "Doe"));
        customerRepository.save(aCustomer(zoeEmail, "Zoe", "Adams"));
        customerRepository.save(aCustomer(aliceEmail, "Alice", "Doe"));

        List<Customer> customers = customerRepository.findAllByOrderByLastNameAscFirstNameAsc();

        assertThat(customers)
                .filteredOn(customer -> Set.of(janeEmail, zoeEmail, aliceEmail).contains(customer.getEmail()))
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
