package com.tbaar.eurder.feature.customer.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByCredentialsId(UUID credentialsId);

    List<Customer> findAllByOrderByLastNameAscFirstNameAsc();
}
