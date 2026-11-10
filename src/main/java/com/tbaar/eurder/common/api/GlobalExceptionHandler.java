package com.tbaar.eurder.common.api;

import com.tbaar.eurder.common.exception.ConflictException;
import com.tbaar.eurder.common.exception.ErrorCode;
import com.tbaar.eurder.common.exception.EurderException;
import com.tbaar.eurder.common.exception.HasErrorCode;
import com.tbaar.eurder.common.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authorization.AuthorityAuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Turns every error into a ProblemDetail (RFC 9457) with two extra properties for the front end:
 * {@code code}, a translatable key of {@link ErrorCode}, and {@code params}, the values to insert in its translation.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String CODE = "code";
    static final String PARAMS = "params";
    static final String ERRORS = "errors";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationFailure(AuthenticationException e) {
        log.warn("Unauthorized: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"Eurder\"")
                .body(problem(HttpStatus.UNAUTHORIZED, "Invalid credentials", authenticationFailureCode(e), Map.of()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException e) {
        log.warn("Not found: {}", e.getMessage());
        return problem(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException e) {
        log.warn("Conflict: {}", e.getMessage());
        return problem(HttpStatus.CONFLICT, e);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleConcurrentModification(OptimisticLockingFailureException e) {
        log.warn("Concurrent modification: {}", e.getMessage());
        return problem(HttpStatus.CONFLICT, "The data was modified by another request, please retry",
                ErrorCode.CONCURRENT_MODIFICATION, Map.of());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ProblemDetail handleAccessDenied(AuthorizationDeniedException e, Principal principal) {
        List<String> features = requiredAuthorities(e);
        String required = features.isEmpty() ? "this resource" : String.join(", ", features);
        log.warn("Forbidden: user {} has no access to {}", principal.getName(), required);
        return problem(HttpStatus.FORBIDDEN, "You have no access to " + required, ErrorCode.FORBIDDEN, Map.of("features", features));
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", ErrorCode.UNEXPECTED, Map.of());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        ProblemDetail body = problem(status, "Validation failed", ErrorCode.VALIDATION_FAILED, Map.of());
        body.setProperty(ERRORS, ValidationErrorMapper.toValidationErrors(e.getBindingResult().getFieldErrors()));
        return handleExceptionInternal(e, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(e, unreadableBodyProblem(e, status), headers, status, request);
    }

    /**
     * Every error Spring MVC handles itself (unknown route, wrong method, wrong parameter type...) also gets a code here.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception e, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        Object completeBody = withFallbackCode(resolvedBody(e, body), e, statusCode);
        if (statusCode.is5xxServerError()) {
            log.error("{} {}", statusCode.value(), e.getClass().getSimpleName(), e);
        } else {
            log.warn("{} {}: {}", statusCode.value(), e.getClass().getSimpleName(), loggableDetail(completeBody));
        }
        return super.handleExceptionInternal(e, completeBody, headers, statusCode, request);
    }

    private Object resolvedBody(Exception e, Object body) {
        if (body == null && e instanceof ErrorResponse errorResponse) {
            return errorResponse.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
        }
        return body;
    }

    private static Object withFallbackCode(Object body, Exception e, HttpStatusCode status) {
        if (body instanceof ProblemDetail problem && !hasCode(problem)) {
            return withCode(problem, fallbackCode(e, status), fallbackParams(e));
        }
        return body;
    }

    private static boolean hasCode(ProblemDetail problem) {
        return problem.getProperties() != null && problem.getProperties().containsKey(CODE);
    }

    private static ErrorCode fallbackCode(Exception e, HttpStatusCode status) {
        if (e instanceof TypeMismatchException) {
            return ErrorCode.INVALID_PARAMETER;
        }
        if (status.value() == HttpStatus.NOT_FOUND.value()) {
            return ErrorCode.ROUTE_NOT_FOUND;
        }
        if (status.value() == HttpStatus.METHOD_NOT_ALLOWED.value()) {
            return ErrorCode.METHOD_NOT_ALLOWED;
        }
        if (status.value() == HttpStatus.NOT_ACCEPTABLE.value()) {
            return ErrorCode.NOT_ACCEPTABLE;
        }
        if (status.value() == HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()) {
            return ErrorCode.UNSUPPORTED_MEDIA_TYPE;
        }
        return status.is4xxClientError() ? ErrorCode.BAD_REQUEST : ErrorCode.UNEXPECTED;
    }

    private static Map<String, Object> fallbackParams(Exception e) {
        return e instanceof MethodArgumentTypeMismatchException mismatch
                ? Map.of("parameter", mismatch.getName())
                : Map.of();
    }

    private static ErrorCode authenticationFailureCode(AuthenticationException e) {
        if (e instanceof HasErrorCode coded) {
            return coded.code();
        }
        if (isExpiredAccessToken(e)) {
            return ErrorCode.ACCESS_TOKEN_EXPIRED;
        }
        // Only the bearer token of a request raises an OAuth2AuthenticationException here: malformed, badly signed, wrong issuer...
        if (e instanceof OAuth2AuthenticationException) {
            return ErrorCode.INVALID_ACCESS_TOKEN;
        }
        if (e instanceof InsufficientAuthenticationException) {
            return ErrorCode.AUTHENTICATION_REQUIRED;
        }
        return ErrorCode.INVALID_CREDENTIALS;
    }

    // The validator of JwtConfig reports an expired access token with the key of ACCESS_TOKEN_EXPIRED as error code.
    private static boolean isExpiredAccessToken(AuthenticationException e) {
        return e.getCause() instanceof JwtValidationException invalidToken
                && invalidToken.getErrors().stream()
                .map(OAuth2Error::getErrorCode)
                .anyMatch(ErrorCode.ACCESS_TOKEN_EXPIRED.key()::equals);
    }

    private static ProblemDetail problem(HttpStatusCode status, EurderException e) {
        return problem(status, e.getMessage(), e.code(), e.params());
    }

    private static ProblemDetail problem(HttpStatusCode status, String detail, ErrorCode code, Map<String, Object> params) {
        return withCode(ProblemDetail.forStatusAndDetail(status, detail), code, params);
    }

    private static ProblemDetail withCode(ProblemDetail problem, ErrorCode code, Map<String, Object> params) {
        problem.setProperty(CODE, code.key());
        problem.setProperty(PARAMS, params);
        return problem;
    }

    private static List<String> requiredAuthorities(AuthorizationDeniedException e) {
        if (e.getAuthorizationResult() instanceof AuthorityAuthorizationDecision decision) {
            return decision.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .sorted()
                    .toList();
        }
        return List.of();
    }

    private static ProblemDetail unreadableBodyProblem(HttpMessageNotReadableException e, HttpStatusCode status) {
        if (e.getCause() instanceof MismatchedInputException mismatch) {
            String field = fieldPath(mismatch.getPath());
            return problem(status, "Invalid value for field '" + field + "'", ErrorCode.INVALID_VALUE, Map.of("field", field));
        }
        return problem(status, "Request body is missing or malformed", ErrorCode.MALFORMED_BODY, Map.of());
    }

    private static String fieldPath(List<JacksonException.Reference> path) {
        return path.stream()
                .map(GlobalExceptionHandler::pathSegment)
                .collect(Collectors.joining())
                .replaceFirst("^\\.", "");
    }

    private static String pathSegment(JacksonException.Reference reference) {
        return reference.getPropertyName() == null
                ? "[" + reference.getIndex() + "]"
                : "." + reference.getPropertyName();
    }

    private static String loggableDetail(Object body) {
        if (body instanceof ProblemDetail problem) {
            return problem.getProperties() == null
                    ? problem.getDetail()
                    : problem.getDetail() + " " + problem.getProperties();
        }
        return "no details";
    }
}
