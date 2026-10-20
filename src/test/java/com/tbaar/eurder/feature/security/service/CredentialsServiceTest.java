package com.tbaar.eurder.feature.security.service;

import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.CredentialsRepository;
import com.tbaar.eurder.feature.security.domain.Role;
import com.tbaar.eurder.feature.security.exception.EmailAlreadyUsedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialsServiceTest {

    @Mock
    private CredentialsRepository credentialsRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CredentialsService credentialsService;

    @Test
    void givenUnusedEmail_whenCreating_thenSavesCredentialsWithHashedPasswordAndRole() {
        when(credentialsRepository.existsByEmail("jane@doe.be")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret123");
        when(credentialsRepository.saveAndFlush(any(Credentials.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Credentials created = credentialsService.create("jane@doe.be", "secret123", Role.CUSTOMER);

        assertThat(created.getEmail()).isEqualTo("jane@doe.be");
        assertThat(created.getPasswordHash()).isEqualTo("hashed-secret123");
        assertThat(created.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void givenEmailWithUppercaseAndSpaces_whenCreating_thenStoresItTrimmedInLowercase() {
        when(credentialsRepository.existsByEmail("jane@doe.be")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret123");
        when(credentialsRepository.saveAndFlush(any(Credentials.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Credentials created = credentialsService.create("  Jane@Doe.BE ", "secret123", Role.CUSTOMER);

        assertThat(created.getEmail()).isEqualTo("jane@doe.be");
    }

    @Test
    void givenEmailAlreadyUsed_whenCreating_thenThrowsEmailAlreadyUsedWithoutHashingNorSaving() {
        when(credentialsRepository.existsByEmail("jane@doe.be")).thenReturn(true);

        Throwable thrown = catchThrowable(() -> credentialsService.create("jane@doe.be", "secret123", Role.CUSTOMER));

        assertThat(thrown)
                .isInstanceOf(EmailAlreadyUsedException.class)
                .hasMessage("Email jane@doe.be is already in use");
        verify(passwordEncoder, never()).encode(anyString());
        verify(credentialsRepository, never()).saveAndFlush(any(Credentials.class));
    }

    @Test
    void givenEmailTakenConcurrentlyAfterTheCheck_whenSaving_thenThrowsEmailAlreadyUsed() {
        when(credentialsRepository.existsByEmail("jane@doe.be")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret123");
        when(credentialsRepository.saveAndFlush(any(Credentials.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint violated on credentials.email"));

        Throwable thrown = catchThrowable(() -> credentialsService.create("jane@doe.be", "secret123", Role.CUSTOMER));

        assertThat(thrown)
                .isInstanceOf(EmailAlreadyUsedException.class)
                .hasMessage("Email jane@doe.be is already in use")
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void givenUsedEmailWrittenWithAnotherCase_whenCreating_thenThrowsEmailAlreadyUsed() {
        when(credentialsRepository.existsByEmail("jane@doe.be")).thenReturn(true);

        Throwable thrown = catchThrowable(() -> credentialsService.create("JANE@doe.be ", "secret123", Role.CUSTOMER));

        assertThat(thrown).isInstanceOf(EmailAlreadyUsedException.class);
    }
}
