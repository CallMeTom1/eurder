package com.tbaar.eurder.common.api;

import com.tbaar.eurder.common.exception.ConflictException;
import com.tbaar.eurder.common.exception.ForbiddenException;
import com.tbaar.eurder.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void givenNotFoundException_whenHandled_thenReturns404WithItsMessage() {
        NotFoundException exception = new NotFoundException("No customer found for id 42");

        ProblemDetail response = handler.handleNotFound(exception);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getDetail()).isEqualTo("No customer found for id 42");
    }

    @Test
    void givenConflictException_whenHandled_thenReturns409WithItsMessage() {
        ConflictException exception = new ConflictException("Email already in use");

        ProblemDetail response = handler.handleConflict(exception);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.getDetail()).isEqualTo("Email already in use");
    }

    @Test
    void givenForbiddenException_whenHandled_thenReturns403WithItsMessage() {
        ForbiddenException exception = new ForbiddenException("Not your order");

        ProblemDetail response = handler.handleForbidden(exception);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getDetail()).isEqualTo("Not your order");
    }

    @Test
    void givenIllegalArgumentException_whenHandled_thenReturns400WithItsMessage() {
        IllegalArgumentException exception = new IllegalArgumentException("Amount must be positive");

        ProblemDetail response = handler.handleBadRequest(exception);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getDetail()).isEqualTo("Amount must be positive");
    }

    @Test
    void givenUnexpectedException_whenHandled_thenReturns500WithoutLeakingItsMessage() {
        RuntimeException exception = new RuntimeException("database password is wrong");

        ProblemDetail response = handler.handleUnexpected(exception);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getDetail()).isEqualTo("An unexpected error occurred");
    }

    @Test
    void givenTwoInvalidFields_whenValidationFails_thenReturns400ListingEachField() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "createCustomerDto");
        bindingResult.addError(new FieldError("createCustomerDto", "email", "must not be blank"));
        bindingResult.addError(new FieldError("createCustomerDto", "firstName", "must not be blank"));
        MethodParameter parameter = new MethodParameter(getClass().getDeclaredMethod("someEndpoint", Object.class), 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(exception, new HttpHeaders(),
                HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOfSatisfying(ProblemDetail.class, body ->
                assertThat(body.getProperties()).containsEntry("errors",
                        Map.of("email", "must not be blank", "firstName", "must not be blank")));
    }

    @SuppressWarnings("unused")
    private void someEndpoint(Object body) {
    }
}
