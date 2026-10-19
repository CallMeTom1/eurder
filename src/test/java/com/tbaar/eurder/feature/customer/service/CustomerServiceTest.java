package com.tbaar.eurder.feature.customer.service;

import com.tbaar.eurder.feature.customer.api.AddressMapper;
import com.tbaar.eurder.feature.customer.api.CustomerMapper;
import com.tbaar.eurder.feature.customer.api.dto.AddressDto;
import com.tbaar.eurder.feature.customer.api.dto.CreateCustomerDto;
import com.tbaar.eurder.feature.customer.api.dto.CustomerDto;
import com.tbaar.eurder.feature.customer.domain.Address;
import com.tbaar.eurder.feature.customer.domain.Customer;
import com.tbaar.eurder.feature.customer.domain.CustomerRepository;
import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.Role;
import com.tbaar.eurder.feature.security.exception.EmailAlreadyUsedException;
import com.tbaar.eurder.feature.security.service.CredentialsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CredentialsService credentialsService;

    private CustomerService customerService;

    @BeforeEach
    void createService() {
        customerService = new CustomerService(customerRepository, credentialsService, new CustomerMapper(new AddressMapper()));
    }

    @Test
    void givenValidRegistration_whenRegistering_thenCreatesCredentialsWithTheCustomerRole() {
        when(credentialsService.create("jane@doe.be", "secret123", Role.CUSTOMER))
                .thenReturn(new Credentials("jane@doe.be", "hash", Role.CUSTOMER));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        customerService.register(registration("jane@doe.be"));

        verify(credentialsService).create("jane@doe.be", "secret123", Role.CUSTOMER);
    }

    @Test
    void givenValidRegistration_whenRegistering_thenSavesTheCustomerAndReturnsItWithTheNormalizedEmail() {
        when(credentialsService.create(" Jane@Doe.BE", "secret123", Role.CUSTOMER))
                .thenReturn(new Credentials("jane@doe.be", "hash", Role.CUSTOMER));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerDto registered = customerService.register(registration(" Jane@Doe.BE"));

        assertThat(registered.email()).isEqualTo("jane@doe.be");
        assertThat(registered.firstName()).isEqualTo("Jane");
        assertThat(registered.address().city()).isEqualTo("Bruxelles");
    }

    @Test
    void givenEmailAlreadyUsed_whenRegistering_thenPropagatesTheConflictAndSavesNoCustomer() {
        when(credentialsService.create("jane@doe.be", "secret123", Role.CUSTOMER))
                .thenThrow(new EmailAlreadyUsedException("jane@doe.be"));

        Throwable thrown = catchThrowable(() -> customerService.register(registration("jane@doe.be")));

        assertThat(thrown).isInstanceOf(EmailAlreadyUsedException.class);
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void givenSortedCustomersInTheRepository_whenFindingAll_thenReturnsTheirDtosInTheSameOrder() {
        when(customerRepository.findAllByOrderByLastNameAscFirstNameAsc()).thenReturn(List.of(
                customer("zoe@adams.be", "Zoe", "Adams"),
                customer("jane@doe.be", "Jane", "Doe")));

        List<CustomerDto> customers = customerService.findAll();

        assertThat(customers)
                .extracting(CustomerDto::email)
                .containsExactly("zoe@adams.be", "jane@doe.be");
    }

    @Test
    void givenNoCustomer_whenFindingAll_thenReturnsAnEmptyList() {
        when(customerRepository.findAllByOrderByLastNameAscFirstNameAsc()).thenReturn(List.of());

        List<CustomerDto> customers = customerService.findAll();

        assertThat(customers).isEmpty();
    }

    private static Customer customer(String email, String firstName, String lastName) {
        return new Customer(new Credentials(email, "hash", Role.CUSTOMER), firstName, lastName,
                new Address("Rue Haute", "1", "1000", "Bruxelles"), "0470123456");
    }

    private static CreateCustomerDto registration(String email) {
        return new CreateCustomerDto("Jane", "Doe", email, "secret123",
                new AddressDto("Rue Haute", "1", "1000", "Bruxelles"), "0470123456");
    }
}
