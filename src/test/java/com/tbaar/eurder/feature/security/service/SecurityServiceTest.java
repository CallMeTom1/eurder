package com.tbaar.eurder.feature.security.service;

import com.tbaar.eurder.common.exception.UnauthorizedException;
import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.CredentialsRepository;
import com.tbaar.eurder.feature.security.domain.Feature;
import com.tbaar.eurder.feature.security.domain.Role;
import com.tbaar.eurder.feature.security.exception.FeatureAccessDeniedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private CredentialsRepository credentialsRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SecurityService securityService;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Bearer some-token", "basic amFuZUBkb2UuYmU6c2VjcmV0MTIz", "Basic !!!not-base64!!!", "Basic bm8tY29sb24="})
    void givenMissingOrMalformedHeader_whenValidating_thenThrowsUnauthorized(String authorization) {
        Throwable thrown = catchThrowable(() -> securityService.validateAuthorization(authorization, Feature.ORDER_ITEMS));

        assertThat(thrown)
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void givenUnknownEmail_whenValidating_thenThrowsUnauthorized() {
        when(credentialsRepository.findByEmail("ghost@doe.be")).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> securityService.validateAuthorization(basic("ghost@doe.be", "secret123"), Feature.ORDER_ITEMS));

        assertThat(thrown)
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void givenWrongPassword_whenValidating_thenThrowsUnauthorizedWithTheSameMessageAsUnknownEmail() {
        Credentials jane = new Credentials("jane@doe.be", "hash-of-secret123", Role.CUSTOMER);
        when(credentialsRepository.findByEmail("jane@doe.be")).thenReturn(Optional.of(jane));
        when(passwordEncoder.matches("wrong-password", "hash-of-secret123")).thenReturn(false);

        Throwable thrown = catchThrowable(() -> securityService.validateAuthorization(basic("jane@doe.be", "wrong-password"), Feature.ORDER_ITEMS));

        assertThat(thrown)
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void givenValidCredentialsAndGrantedFeature_whenValidating_thenReturnsTheAuthenticatedCredentials() {
        Credentials jane = new Credentials("jane@doe.be", "hash-of-secret123", Role.CUSTOMER);
        when(credentialsRepository.findByEmail("jane@doe.be")).thenReturn(Optional.of(jane));
        when(passwordEncoder.matches("secret123", "hash-of-secret123")).thenReturn(true);

        Credentials authenticated = securityService.validateAuthorization(basic("jane@doe.be", "secret123"), Feature.ORDER_ITEMS);

        assertThat(authenticated).isSameAs(jane);
    }

    @Test
    void givenEmailWrittenWithUppercaseAndSpaces_whenValidating_thenLooksItUpNormalized() {
        Credentials jane = new Credentials("jane@doe.be", "hash-of-secret123", Role.CUSTOMER);
        when(credentialsRepository.findByEmail("jane@doe.be")).thenReturn(Optional.of(jane));
        when(passwordEncoder.matches("secret123", "hash-of-secret123")).thenReturn(true);

        Credentials authenticated = securityService.validateAuthorization(basic(" Jane@Doe.BE", "secret123"), Feature.ORDER_ITEMS);

        assertThat(authenticated).isSameAs(jane);
    }

    @Test
    void givenPasswordContainingColons_whenValidating_thenKeepsEverythingAfterTheFirstColonAsPassword() {
        Credentials jane = new Credentials("jane@doe.be", "hash-of-pass-with-colons", Role.CUSTOMER);
        when(credentialsRepository.findByEmail("jane@doe.be")).thenReturn(Optional.of(jane));
        when(passwordEncoder.matches("pass:with:colons", "hash-of-pass-with-colons")).thenReturn(true);

        Credentials authenticated = securityService.validateAuthorization(basic("jane@doe.be", "pass:with:colons"), Feature.ORDER_ITEMS);

        assertThat(authenticated).isSameAs(jane);
    }

    @Test
    void givenValidCredentialsWithoutTheFeature_whenValidating_thenThrowsFeatureAccessDenied() {
        Credentials jane = new Credentials("jane@doe.be", "hash-of-secret123", Role.CUSTOMER);
        when(credentialsRepository.findByEmail("jane@doe.be")).thenReturn(Optional.of(jane));
        when(passwordEncoder.matches("secret123", "hash-of-secret123")).thenReturn(true);

        Throwable thrown = catchThrowable(() -> securityService.validateAuthorization(basic("jane@doe.be", "secret123"), Feature.MANAGE_ITEMS));

        assertThat(thrown)
                .isInstanceOf(FeatureAccessDeniedException.class)
                .hasMessage("User jane@doe.be has no access to MANAGE_ITEMS");
    }

    private static String basic(String email, String password) {
        String credentials = email + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
