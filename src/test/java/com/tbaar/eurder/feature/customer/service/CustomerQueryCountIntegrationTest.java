package com.tbaar.eurder.feature.customer.service;

import com.tbaar.eurder.feature.customer.api.dto.AddressDto;
import com.tbaar.eurder.feature.customer.api.dto.CreateCustomerDto;
import com.tbaar.eurder.feature.customer.domain.CustomerRepository;
import com.tbaar.eurder.feature.security.domain.CredentialsRepository;
import com.tbaar.eurder.feature.security.domain.Role;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class CustomerQueryCountIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @AfterEach
    void deleteRegisteredCustomers() {
        customerRepository.deleteAll();
        credentialsRepository.deleteAllByRole(Role.CUSTOMER);
    }

    @Test
    void givenThreeCustomers_whenFindingAll_thenLoadsThemWithTheirCredentialsInASingleQuery() {
        registerCustomer("adams@doe.be", "Adams");
        registerCustomer("brown@doe.be", "Brown");
        registerCustomer("clark@doe.be", "Clark");
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        customerService.findAll();

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    private void registerCustomer(String email, String lastName) {
        customerService.register(new CreateCustomerDto("Jane", lastName, email, "secret123",
                new AddressDto("Rue Haute", "1", "1000", "Bruxelles"), "0470123456"));
    }
}
