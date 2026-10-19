package com.tbaar.eurder.feature.customer.service;

import com.tbaar.eurder.feature.customer.api.CustomerMapper;
import com.tbaar.eurder.feature.customer.api.dto.CreateCustomerDto;
import com.tbaar.eurder.feature.customer.api.dto.CustomerDto;
import com.tbaar.eurder.feature.customer.domain.Customer;
import com.tbaar.eurder.feature.customer.domain.CustomerRepository;
import com.tbaar.eurder.feature.customer.exception.CustomerNotFoundException;
import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.Role;
import com.tbaar.eurder.feature.security.service.CredentialsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CredentialsService credentialsService;
    private final CustomerMapper customerMapper;

    public CustomerService(CustomerRepository customerRepository, CredentialsService credentialsService, CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.credentialsService = credentialsService;
        this.customerMapper = customerMapper;
    }

    public CustomerDto register(CreateCustomerDto request) {
        Credentials credentials = credentialsService.create(request.email(), request.password(), Role.CUSTOMER);
        Customer customer = customerRepository.save(customerMapper.toEntity(request, credentials));
        return customerMapper.toDto(customer);
    }

    @Transactional(readOnly = true)
    public CustomerDto findById(UUID id) {
        return customerRepository.findById(id)
                .map(customerMapper::toDto)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> findAll() {
        return customerRepository.findAllByOrderByLastNameAscFirstNameAsc().stream()
                .map(customerMapper::toDto)
                .toList();
    }
}
