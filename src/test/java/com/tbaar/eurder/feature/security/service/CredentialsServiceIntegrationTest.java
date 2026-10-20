package com.tbaar.eurder.feature.security.service;

import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.CredentialsRepository;
import com.tbaar.eurder.feature.security.domain.Role;
import com.tbaar.eurder.feature.security.exception.EmailAlreadyUsedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

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

    @Test
    void givenNewCustomerCredentials_whenCreating_thenTheyAreStoredWithAVerifiableHash() {
        String rawPassword = "secret123";

        credentialsService.create(" Jane@Doe.be", rawPassword, Role.CUSTOMER);

        Optional<Credentials> stored = credentialsRepository.findByEmail("jane@doe.be");
        assertThat(stored).hasValueSatisfying(credentials -> {
            assertThat(credentials.getRole()).isEqualTo(Role.CUSTOMER);
            assertThat(passwordEncoder.matches(rawPassword, credentials.getPasswordHash())).isTrue();
        });
    }

    @Test
    void givenExistingCredentials_whenCreatingWithTheSameEmailInAnotherCase_thenThrowsEmailAlreadyUsed() {
        credentialsService.create("jane@doe.be", "secret123", Role.CUSTOMER);
        long credentialsBefore = credentialsRepository.count();

        Throwable thrown = catchThrowable(() -> credentialsService.create("JANE@DOE.BE", "other-password", Role.CUSTOMER));

        assertThat(thrown).isInstanceOf(EmailAlreadyUsedException.class);
        assertThat(credentialsRepository.count()).isEqualTo(credentialsBefore);
    }
}
