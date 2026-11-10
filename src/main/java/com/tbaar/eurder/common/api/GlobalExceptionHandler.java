package com.tbaar.eurder.common.api;

import com.tbaar.eurder.common.exception.ConflictException;
import com.tbaar.eurder.common.exception.ErrorCode;
import com.tbaar.eurder.common.exception.EurderException;
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
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tbaar.eurder.common.api.ProblemDetails.hasCode;
import static com.tbaar.eurder.common.api.ProblemDetails.problem;
import static com.tbaar.eurder.common.api.ProblemDetails.withCode;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final Map<Integer, ErrorCode> CODES_BY_STATUS = Map.of(
            HttpStatus.NOT_FOUND.value(), ErrorCode.ROUTE_NOT_FOUND,
            HttpStatus.METHOD_NOT_ALLOWED.value(), ErrorCode.METHOD_NOT_ALLOWED,
            HttpStatus.NOT_ACCEPTABLE.value(), ErrorCode.NOT_ACCEPTABLE,
            HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), ErrorCode.UNSUPPORTED_MEDIA_TYPE);

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException e) {
        log.warn("Not found: {}", e.getMessage());
        return businessProblem(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException e) {
        log.warn("Conflict: {}", e.getMessage());
        return businessProblem(HttpStatus.CONFLICT, e);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleConcurrentModification(OptimisticLockingFailureException e) {
        log.warn("Concurrent modification: {}", e.getMessage());
        return problem(HttpStatus.CONFLICT, "The data was modified by another request, please retry",
                ErrorCode.CONCURRENT_MODIFICATION, Map.of());
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
        body.setProperty(ProblemDetails.ERRORS, ValidationErrorMapper.toValidationErrors(e.getBindingResult().getFieldErrors()));
        return handleExceptionInternal(e, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(e, unreadableBodyProblem(e, status), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception e, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        Object completeBody = addCodeIfMissing(buildBodyIfMissing(e, body), e, statusCode);
        if (statusCode.is5xxServerError()) {
            log.error("{} {}", statusCode.value(), e.getClass().getSimpleName(), e);
        } else {
            log.warn("{} {}: {}", statusCode.value(), e.getClass().getSimpleName(), loggableDetail(completeBody));
        }
        return super.handleExceptionInternal(e, completeBody, headers, statusCode, request);
    }

    private static ProblemDetail businessProblem(HttpStatusCode status, EurderException e) {
        return problem(status, e.getMessage(), e.code(), e.params());
    }

    private Object buildBodyIfMissing(Exception e, Object body) {
        if (body == null && e instanceof ErrorResponse errorResponse) {
            return errorResponse.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
        }
        return body;
    }

    private static Object addCodeIfMissing(Object body, Exception e, HttpStatusCode status) {
        if (body instanceof ProblemDetail problem && !hasCode(problem)) {
            return withCode(problem, codeOfSpringError(e, status), paramsOfSpringError(e));
        }
        return body;
    }

    private static ErrorCode codeOfSpringError(Exception e, HttpStatusCode status) {
        if (e instanceof TypeMismatchException) {
            return ErrorCode.INVALID_PARAMETER;
        }
        ErrorCode defaultCode = status.is4xxClientError() ? ErrorCode.BAD_REQUEST : ErrorCode.UNEXPECTED;
        return CODES_BY_STATUS.getOrDefault(status.value(), defaultCode);
    }

    private static Map<String, Object> paramsOfSpringError(Exception e) {
        return e instanceof MethodArgumentTypeMismatchException mismatch
                ? Map.of("parameter", mismatch.getName())
                : Map.of();
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
