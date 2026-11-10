package com.tbaar.eurder.feature.security.api;

import com.tbaar.eurder.common.exception.ErrorCode;
import com.tbaar.eurder.common.exception.HasErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authorization.AuthorityAuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import static com.tbaar.eurder.common.api.ProblemDetails.problem;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SecurityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SecurityExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationFailure(AuthenticationException e) {
        log.warn("Unauthorized: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"Eurder\"")
                .body(problem(HttpStatus.UNAUTHORIZED, "Invalid credentials", authenticationFailureCode(e), Map.of()));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ProblemDetail handleAccessDenied(AuthorizationDeniedException e, Principal principal) {
        List<String> missingFeatures = missingFeatures(e);
        String required = missingFeatures.isEmpty() ? "this resource" : String.join(", ", missingFeatures);
        log.warn("Forbidden: user {} has no access to {}", principal.getName(), required);
        return problem(HttpStatus.FORBIDDEN, "You have no access to " + required, ErrorCode.FORBIDDEN, Map.of("features", missingFeatures));
    }

    private static ErrorCode authenticationFailureCode(AuthenticationException e) {
        if (e instanceof HasErrorCode exceptionWithCode) {
            return exceptionWithCode.code();
        }
        if (isExpiredAccessToken(e)) {
            return ErrorCode.ACCESS_TOKEN_EXPIRED;
        }
        if (isInvalidAccessToken(e)) {
            return ErrorCode.INVALID_ACCESS_TOKEN;
        }
        if (isMissingAccessToken(e)) {
            return ErrorCode.AUTHENTICATION_REQUIRED;
        }
        return ErrorCode.INVALID_CREDENTIALS;
    }

    private static boolean isExpiredAccessToken(AuthenticationException e) {
        return e.getCause() instanceof JwtValidationException invalidToken
                && invalidToken.getErrors().stream()
                .map(OAuth2Error::getErrorCode)
                .anyMatch(ErrorCode.ACCESS_TOKEN_EXPIRED.key()::equals);
    }

    private static boolean isInvalidAccessToken(AuthenticationException e) {
        return e instanceof OAuth2AuthenticationException;
    }

    private static boolean isMissingAccessToken(AuthenticationException e) {
        return e instanceof InsufficientAuthenticationException;
    }

    private static List<String> missingFeatures(AuthorizationDeniedException e) {
        if (e.getAuthorizationResult() instanceof AuthorityAuthorizationDecision decision) {
            return decision.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .sorted()
                    .toList();
        }
        return List.of();
    }
}
