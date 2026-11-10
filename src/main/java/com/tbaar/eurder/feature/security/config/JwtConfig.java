package com.tbaar.eurder.feature.security.config;

import com.tbaar.eurder.common.exception.ErrorCode;
import com.tbaar.eurder.feature.security.authentication.AccessTokenIssuer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

@Configuration
public class JwtConfig {

    private static final int MINIMUM_SECRET_BYTES = 32;

    @Bean
    public SecretKey jwtSecretKey(@Value("${eurder.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least " + MINIMUM_SECRET_BYTES + " bytes long");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey).build();
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator notExpired = new JwtTimestampValidator(Duration.ZERO);
        notExpired.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                notExpired, expiryReportedToTheFrontEnd(clock), new JwtIssuerValidator(AccessTokenIssuer.ISSUER)));
        return decoder;
    }

    // JwtTimestampValidator reports an expiry like any invalid token: this error tells the front end to refresh it.
    static OAuth2TokenValidator<Jwt> expiryReportedToTheFrontEnd(Clock clock) {
        OAuth2Error expired = new OAuth2Error(ErrorCode.ACCESS_TOKEN_EXPIRED.key(), "The access token has expired", null);
        return jwt -> jwt.getExpiresAt() != null && clock.instant().isAfter(jwt.getExpiresAt())
                ? OAuth2TokenValidatorResult.failure(expired)
                : OAuth2TokenValidatorResult.success();
    }
}
