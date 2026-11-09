package com.tbaar.eurder.feature.customer.domain;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends MongoRepository<Customer, UUID> {

    Optional<Customer> findByCredentialsId(UUID credentialsId);

    List<Customer> findAllByOrderByLastNameAscFirstNameAsc();
}
