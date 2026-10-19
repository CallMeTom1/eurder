package com.tbaar.eurder.feature.security.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new PasswordEncoder();

    @Test
    void givenRawPassword_whenEncoding_thenReturnsBcryptHashDifferentFromThePassword() {
        String rawPassword = "secret";

        String hash = passwordEncoder.encode(rawPassword);

        assertThat(hash)
                .isNotEqualTo(rawPassword)
                .startsWith("$2a$10$");
    }

    @Test
    void givenSamePasswordEncodedTwice_whenComparingHashes_thenTheyDifferBecauseOfTheSalt() {
        String rawPassword = "secret";

        String firstHash = passwordEncoder.encode(rawPassword);
        String secondHash = passwordEncoder.encode(rawPassword);

        assertThat(firstHash).isNotEqualTo(secondHash);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void givenBlankPassword_whenEncoding_thenThrowsIllegalArgument(String blankPassword) {
        Throwable thrown = catchThrowable(() -> passwordEncoder.encode(blankPassword));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenHashOfPassword_whenMatchingTheSamePassword_thenReturnsTrue() {
        String hash = passwordEncoder.encode("secret");

        boolean matches = passwordEncoder.matches("secret", hash);

        assertThat(matches).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"wrong", "Secret", "secret "})
    void givenHashOfPassword_whenMatchingAnotherPassword_thenReturnsFalse(String otherPassword) {
        String hash = passwordEncoder.encode("secret");

        boolean matches = passwordEncoder.matches(otherPassword, hash);

        assertThat(matches).isFalse();
    }
}
