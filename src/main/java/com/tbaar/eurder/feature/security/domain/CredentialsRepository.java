package com.tbaar.eurder.feature.security.domain;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface CredentialsRepository extends MongoRepository<Credentials, UUID> {

    Optional<Credentials> findByEmail(String email);

    boolean existsByEmail(String email);

    @Transactional
    long deleteAllByRole(Role role);
}
