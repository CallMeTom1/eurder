package com.tbaar.eurder.feature.security.service;

import com.tbaar.eurder.common.exception.UnauthorizedException;
import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.Feature;
import com.tbaar.eurder.feature.security.domain.Role;
import com.tbaar.eurder.feature.security.exception.FeatureAccessDeniedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
class SecurityServiceIntegrationTest {

    @Autowired
    private SecurityService securityService;

    @Test
    void givenDefaultAdminCredentials_whenRequestingAnAdminFeature_thenAuthenticatesTheAdmin() {
        String authorization = basic("admin@eurder.be", "admin");

        Credentials authenticated = securityService.validateAuthorization(authorization, Feature.MANAGE_ITEMS);

        assertThat(authenticated.getEmail()).isEqualTo("admin@eurder.be");
        assertThat(authenticated.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void givenDefaultAdminCredentials_whenRequestingACustomerFeature_thenThrowsFeatureAccessDenied() {
        String authorization = basic("admin@eurder.be", "admin");

        Throwable thrown = catchThrowable(() -> securityService.validateAuthorization(authorization, Feature.ORDER_ITEMS));

        assertThat(thrown).isInstanceOf(FeatureAccessDeniedException.class);
    }

    @Test
    void givenDefaultAdminWithWrongPassword_whenValidating_thenThrowsUnauthorized() {
        String authorization = basic("admin@eurder.be", "not-admin");

        Throwable thrown = catchThrowable(() -> securityService.validateAuthorization(authorization, Feature.MANAGE_ITEMS));

        assertThat(thrown).isInstanceOf(UnauthorizedException.class);
    }

    private static String basic(String email, String password) {
        String credentials = email + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
