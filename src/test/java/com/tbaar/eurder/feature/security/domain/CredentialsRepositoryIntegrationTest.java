package com.tbaar.eurder.feature.security.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CredentialsRepositoryIntegrationTest {

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Test
    void givenSavedCredentials_whenFindingByEmail_thenReturnsThem() {
        Credentials saved = credentialsRepository.save(new Credentials("jane@doe.be", "hashed-password", Role.CUSTOMER));

        Optional<Credentials> found = credentialsRepository.findByEmail("jane@doe.be");

        assertThat(found).hasValueSatisfying(credentials -> {
            assertThat(credentials.getId()).isEqualTo(saved.getId());
            assertThat(credentials.getPasswordHash()).isEqualTo("hashed-password");
            assertThat(credentials.getRole()).isEqualTo(Role.CUSTOMER);
        });
    }
}
