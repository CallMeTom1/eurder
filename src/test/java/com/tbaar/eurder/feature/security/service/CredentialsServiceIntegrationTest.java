package com.tbaar.eurder.feature.security.service;

import com.tbaar.eurder.common.UniqueEmail;
import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.CredentialsRepository;
import com.tbaar.eurder.feature.security.domain.Role;
import com.tbaar.eurder.feature.security.exception.EmailAlreadyUsedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@Transactional
class CredentialsServiceIntegrationTest {

    @Autowired
    private CredentialsService credentialsService;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String email = UniqueEmail.of("jane");

    @Test
    void givenNewCustomerCredentials_whenCreating_thenTheyAreStoredWithAVerifiableHash() {
        String rawPassword = "secret123";

        credentialsService.create(" " + email.toUpperCase(Locale.ROOT), rawPassword, Role.CUSTOMER);

        Optional<Credentials> stored = credentialsRepository.findByEmail(email);
        assertThat(stored).hasValueSatisfying(credentials -> {
            assertThat(credentials.getRole()).isEqualTo(Role.CUSTOMER);
            assertThat(passwordEncoder.matches(rawPassword, credentials.getPasswordHash())).isTrue();
        });
    }

    @Test
    void givenExistingCredentials_whenCreatingWithTheSameEmailInAnotherCase_thenThrowsEmailAlreadyUsedAndKeepsTheFirstPassword() {
        credentialsService.create(email, "secret123", Role.CUSTOMER);

        Throwable thrown = catchThrowable(() -> credentialsService.create(email.toUpperCase(Locale.ROOT), "other-password", Role.CUSTOMER));

        assertThat(thrown).isInstanceOf(EmailAlreadyUsedException.class);
        assertThat(credentialsRepository.findByEmail(email)).hasValueSatisfying(credentials ->
                assertThat(passwordEncoder.matches("secret123", credentials.getPasswordHash())).isTrue());
    }
}
