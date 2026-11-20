package com.tbaar.eurder.feature.customer.service;

import com.tbaar.eurder.feature.customer.domain.CustomerRepository;
import com.tbaar.eurder.feature.security.service.DisplayName;
import com.tbaar.eurder.feature.security.service.DisplayNames;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class CustomerDisplayNames implements DisplayNames {

    private final CustomerRepository customerRepository;

    public CustomerDisplayNames(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DisplayName> findByCredentialsId(UUID credentialsId) {
        return customerRepository.findByCredentialsId(credentialsId)
                .map(customer -> new DisplayName(customer.getFirstName(), customer.getLastName()));
    }
}
