package com.tbaar.eurder.feature.security.api;

import com.tbaar.eurder.common.exception.ErrorCode;
import com.tbaar.eurder.feature.security.exception.InvalidRefreshTokenException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authorization.AuthorityAuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import static com.tbaar.eurder.common.api.ProblemDetailAssertions.assertCode;
import static org.assertj.core.api.Assertions.assertThat;

class SecurityExceptionHandlerTest {

    private final SecurityExceptionHandler handler = new SecurityExceptionHandler();

    @Test
    void givenAFailedAuthentication_whenHandled_thenReturns401WithABearerChallengeWithoutLeakingTheReason() {
        BadCredentialsException exception = new BadCredentialsException("Bad credentials for jane@doe.be");

        ResponseEntity<ProblemDetail> response = handler.handleAuthenticationFailure(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer realm=\"Eurder\"");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Invalid credentials");
        assertCode(response.getBody(), ErrorCode.INVALID_CREDENTIALS, Map.of());
    }

    @Test
    void givenNoAccessToken_whenHandled_thenTellsTheFrontEndToAuthenticate() {
        assertCode(authenticationFailure(new InsufficientAuthenticationException("Full authentication is required")),
                ErrorCode.AUTHENTICATION_REQUIRED, Map.of());
    }

    @Test
    void givenAnInvalidAccessToken_whenHandled_thenReturnsTheInvalidAccessTokenCode() {
        assertCode(authenticationFailure(new InvalidBearerTokenException("Malformed token")), ErrorCode.INVALID_ACCESS_TOKEN, Map.of());
    }

    @Test
    void givenAMalformedBearerHeader_whenHandled_thenReturnsTheInvalidAccessTokenCode() {
        OAuth2AuthenticationException malformed = new OAuth2AuthenticationException(new OAuth2Error("invalid_token"), "Bearer token is malformed");

        assertCode(authenticationFailure(malformed), ErrorCode.INVALID_ACCESS_TOKEN, Map.of());
    }

    @Test
    void givenAnExpiredAccessToken_whenHandled_thenTellsTheFrontEndToRefreshIt() {
        JwtValidationException expired = new JwtValidationException("Jwt expired",
                List.of(new OAuth2Error(ErrorCode.ACCESS_TOKEN_EXPIRED.key(), "The access token has expired", null)));

        assertCode(authenticationFailure(new InvalidBearerTokenException("Jwt expired", expired)), ErrorCode.ACCESS_TOKEN_EXPIRED, Map.of());
    }

    @Test
    void givenAnAccessTokenOfAnotherIssuer_whenHandled_thenReturnsTheInvalidAccessTokenCode() {
        JwtValidationException wrongIssuer = new JwtValidationException("Wrong issuer", List.of(new OAuth2Error("invalid_token")));

        assertCode(authenticationFailure(new InvalidBearerTokenException("Wrong issuer", wrongIssuer)), ErrorCode.INVALID_ACCESS_TOKEN, Map.of());
    }

    @Test
    void givenAnInvalidRefreshToken_whenHandled_thenUsesTheCodeOfTheException() {
        assertCode(authenticationFailure(new InvalidRefreshTokenException()), ErrorCode.INVALID_REFRESH_TOKEN, Map.of());
    }

    @Test
    void givenAccessDeniedByAMissingAuthority_whenHandled_thenReturns403NamingTheAuthority() {
        AuthorizationDeniedException exception = new AuthorizationDeniedException("Access Denied",
                new AuthorityAuthorizationDecision(false, AuthorityUtils.createAuthorityList("ORDER_ITEMS")));
        Principal jane = () -> "jane@doe.be";

        ProblemDetail response = handler.handleAccessDenied(exception, jane);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getDetail()).isEqualTo("You have no access to ORDER_ITEMS");
        assertCode(response, ErrorCode.FORBIDDEN, Map.of("features", List.of("ORDER_ITEMS")));
    }

    private ProblemDetail authenticationFailure(AuthenticationException exception) {
        ProblemDetail body = handler.handleAuthenticationFailure(exception).getBody();
        assertThat(body).isNotNull();
        return body;
    }
}
