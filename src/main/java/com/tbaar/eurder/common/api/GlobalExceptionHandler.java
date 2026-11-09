package com.tbaar.eurder.common.api;

import com.tbaar.eurder.common.exception.ConflictException;
import com.tbaar.eurder.common.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorityAuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationFailure(AuthenticationException e) {
        log.warn("Unauthorized: {}", e.getMessage());
        return unauthorized();
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException e) {
        log.warn("Not found: {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException e) {
        log.warn("Conflict: {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    public ProblemDetail handleConcurrentModification(DataAccessException e) {
        log.warn("Concurrent modification: {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The data was modified by another request, please retry");
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ProblemDetail handleAccessDenied(AuthorizationDeniedException e, Principal principal) {
        String detail = "User " + principal.getName() + " has no access to " + requiredAuthorities(e);
        log.warn("Forbidden: {}", detail);
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, detail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException e) {
        log.warn("Bad request: {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, "Validation failed");
        body.setProperty("errors", errorsByField(e));
        return handleExceptionInternal(e, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, unreadableBodyDetail(e));
        return handleExceptionInternal(e, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception e, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error("{} {}", statusCode.value(), e.getClass().getSimpleName(), e);
        } else {
            log.warn("{} {}: {}", statusCode.value(), e.getClass().getSimpleName(), loggableDetail(body));
        }
        return super.handleExceptionInternal(e, body, headers, statusCode, request);
    }

    private static ResponseEntity<ProblemDetail> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Eurder\"")
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    }

    private static String requiredAuthorities(AuthorizationDeniedException e) {
        if (e.getAuthorizationResult() instanceof AuthorityAuthorizationDecision decision) {
            return decision.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .sorted()
                    .collect(Collectors.joining(", "));
        }
        return "this resource";
    }

    private static Map<String, List<String>> errorsByField(MethodArgumentNotValidException e) {
        return e.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getDefaultMessage))
                .collect(Collectors.groupingBy(FieldError::getField, TreeMap::new,
                        Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())));
    }

    private static String unreadableBodyDetail(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof MismatchedInputException mismatch) {
            return "Invalid value for field '" + fieldPath(mismatch.getPath()) + "'";
        }
        return "Request body is missing or malformed";
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
