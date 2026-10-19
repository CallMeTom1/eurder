package com.tbaar.eurder.feature.customer.service;

import com.tbaar.eurder.feature.customer.api.dto.AddressDto;
import com.tbaar.eurder.feature.customer.api.dto.CreateCustomerDto;
import com.tbaar.eurder.feature.customer.api.dto.CustomerDto;
import com.tbaar.eurder.feature.customer.domain.CustomerRepository;
import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.CredentialsRepository;
import com.tbaar.eurder.feature.security.domain.Feature;
import com.tbaar.eurder.feature.security.domain.Role;
import com.tbaar.eurder.feature.security.service.SecurityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
class CustomerServiceIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private SecurityService securityService;

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
        Credentials authenticated = securityService.validateAuthorization(basic("jane@doe.be", "secret123"), Feature.ORDER_ITEMS);
        assertThat(authenticated.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void givenCustomerRejectedByTheDatabase_whenRegistering_thenNoCredentialsAreLeftBehind() {
        CreateCustomerDto registrationWithTooLongHouseNumber = registration("1".repeat(21));

        Throwable thrown = catchThrowable(() -> customerService.register(registrationWithTooLongHouseNumber));

        assertThat(thrown).isNotNull();
        assertThat(credentialsRepository.findByEmail("jane@doe.be")).isEmpty();
    }

    private static CreateCustomerDto registration(String houseNumber) {
        return new CreateCustomerDto("Jane", "Doe", "jane@doe.be", "secret123",
                new AddressDto("Rue Haute", houseNumber, "1000", "Bruxelles"), "0470123456");
    }

    private static String basic(String email, String password) {
        String credentials = email + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
