package com.tbaar.eurder.feature.customer.service;

import com.tbaar.eurder.feature.customer.api.dto.AddressDto;
import com.tbaar.eurder.feature.customer.api.dto.CreateCustomerDto;
import com.tbaar.eurder.feature.customer.api.dto.CustomerDto;
import com.tbaar.eurder.feature.customer.domain.CustomerRepository;
import com.tbaar.eurder.feature.security.domain.CredentialsRepository;
import com.tbaar.eurder.feature.security.domain.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
class CustomerServiceIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @AfterEach
    void deleteRegisteredCustomers() {
        customerRepository.deleteAll();
        credentialsRepository.deleteAllByRole(Role.CUSTOMER);
    }

    @Test
    void givenValidRegistration_whenRegistering_thenTheCustomerIsStoredAndCanAuthenticate() {
        CreateCustomerDto registration = registration("1");

        CustomerDto registered = customerService.register(registration);

        assertThat(customerRepository.findById(registered.id())).isPresent();
        UserDetails authenticated = userDetailsService.loadUserByUsername("jane@doe.be");
        assertThat(passwordEncoder.matches("secret123", authenticated.getPassword())).isTrue();
        assertThat(authenticated.getAuthorities()).extracting(GrantedAuthority::getAuthority).contains("ORDER_ITEMS");
    }

    @Test
    void givenCustomerRejectedByTheDatabase_whenRegistering_thenNoCredentialsAreLeftBehind() {
        CreateCustomerDto registrationWithTooLongHouseNumber = registration("1".repeat(21));

        Throwable thrown = catchThrowable(() -> customerService.register(registrationWithTooLongHouseNumber));

        assertThat(thrown).isNotNull();
        assertThat(credentialsRepository.findByEmail("jane@doe.be")).isEmpty();
    }

    @Test
    void givenRegisteredCustomers_whenFindingAll_thenReturnsThemSortedWithTheEmailOfTheirCredentials() {
        customerService.register(registration("jane@doe.be", "Jane", "Doe"));
        customerService.register(registration("zoe@adams.be", "Zoe", "Adams"));

        List<CustomerDto> customers = customerService.findAll();

        assertThat(customers)
                .extracting(CustomerDto::lastName, CustomerDto::email)
                .containsExactly(tuple("Adams", "zoe@adams.be"), tuple("Doe", "jane@doe.be"));
    }

    private static CreateCustomerDto registration(String houseNumber) {
        return new CreateCustomerDto("Jane", "Doe", "jane@doe.be", "secret123",
                new AddressDto("Rue Haute", houseNumber, "1000", "Bruxelles"), "0470123456");
    }

    private static CreateCustomerDto registration(String email, String firstName, String lastName) {
        return new CreateCustomerDto(firstName, lastName, email, "secret123",
                new AddressDto("Rue Haute", "1", "1000", "Bruxelles"), "0470123456");
    }
}
