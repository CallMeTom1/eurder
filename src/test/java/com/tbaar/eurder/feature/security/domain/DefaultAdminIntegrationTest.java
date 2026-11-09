package com.tbaar.eurder.feature.security.domain;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DefaultAdminIntegrationTest {

    @Autowired
    private CredentialsRepository credentialsRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void givenAStartedApplication_whenLookingUpTheDefaultAdmin_thenItExistsWithPasswordAdmin() {
        String adminEmail = "admin@eurder.be";

        Optional<Credentials> admin = credentialsRepository.findByEmail(adminEmail);

        assertThat(admin).hasValueSatisfying(credentials -> {
            assertThat(credentials.getRole()).isEqualTo(Role.ADMIN);
            assertThat(passwordEncoder.matches("admin", credentials.getPasswordHash())).isTrue();
        });
    }
}
