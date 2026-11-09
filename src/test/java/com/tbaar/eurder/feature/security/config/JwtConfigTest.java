package com.tbaar.eurder.feature.security.config;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class JwtConfigTest {

    private final JwtConfig jwtConfig = new JwtConfig();

    @Test
    void givenASecretOf32Bytes_whenCreatingTheKey_thenAcceptsIt() {
        SecretKey key = jwtConfig.jwtSecretKey("a".repeat(32));

        assertThat(key.getAlgorithm()).isEqualTo("HmacSHA256");
    }

    @Test
    void givenATooShortSecret_whenCreatingTheKey_thenRefusesWithoutShowingIt() {
        Throwable thrown = catchThrowable(() -> jwtConfig.jwtSecretKey("short-secret"));

        assertThat(thrown)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_SECRET must be at least 32 bytes long");
    }
}
