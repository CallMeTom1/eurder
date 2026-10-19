package com.tbaar.eurder.feature.security.domain;

import com.tbaar.eurder.feature.security.service.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DefaultAdminIntegrationTest {

    @Autowired
    private CredentialsRepository credentialsRepository;

    private final PasswordEncoder passwordEncoder = new PasswordEncoder();

    @Test
    void givenFreshlyMigratedDatabase_whenLookingUpTheDefaultAdmin_thenItExistsWithPasswordAdmin() {
        String adminEmail = "admin@eurder.be";

        Optional<Credentials> admin = credentialsRepository.findByEmail(adminEmail);

        assertThat(admin).hasValueSatisfying(credentials -> {
            assertThat(credentials.getRole()).isEqualTo(Role.ADMIN);
            assertThat(passwordEncoder.matches("admin", credentials.getPasswordHash())).isTrue();
        });
    }
}
