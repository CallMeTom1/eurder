package com.tbaar.eurder.common.api;

import com.tbaar.eurder.common.exception.ErrorCode;
import com.tbaar.eurder.feature.item.exception.ItemNotFoundException;
import com.tbaar.eurder.feature.security.exception.EmailAlreadyUsedException;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tbaar.eurder.common.api.ProblemDetailAssertions.assertCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class GlobalExceptionHandlerTest {

    private static final UUID ID = UUID.fromString("3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90");

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final WebRequest request = new ServletWebRequest(new MockHttpServletRequest());

    @Test
    void givenANotFoundException_whenHandled_thenReturns404WithItsMessageCodeAndParams() {
        ProblemDetail response = handler.handleNotFound(new ItemNotFoundException(ID));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getDetail()).isEqualTo("No item found for id " + ID);
        assertCode(response, ErrorCode.ITEM_NOT_FOUND, Map.of("id", ID));
    }

    @Test
    void givenAConflictException_whenHandled_thenReturns409WithItsMessageCodeAndParams() {
        ProblemDetail response = handler.handleConflict(new EmailAlreadyUsedException("jane@doe.be"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.getDetail()).isEqualTo("Email jane@doe.be is already in use");
        assertCode(response, ErrorCode.EMAIL_ALREADY_USED, Map.of("email", "jane@doe.be"));
    }

    @Test
    void givenAnUnexpectedException_whenHandled_thenReturns500WithoutLeakingItsMessage() {
        ProblemDetail response = handler.handleUnexpected(new RuntimeException("database password is wrong"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getDetail()).isEqualTo("An unexpected error occurred");
        assertCode(response, ErrorCode.UNEXPECTED, Map.of());
    }

    @Test
    void givenABrokenInvariant_whenHandled_thenReturns500InsteadOfBlamingTheClient() {
        ProblemDetail response = handler.handleUnexpected(new IllegalArgumentException("Stock must not be negative: -1"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertCode(response, ErrorCode.UNEXPECTED, Map.of());
    }

    @Test
    void givenAnOptimisticLockingFailure_whenHandled_thenReturns409AskingTheClientToRetry() {
        ProblemDetail response = handler.handleConcurrentModification(new OptimisticLockingFailureException("Row was updated"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.getDetail()).isEqualTo("The data was modified by another request, please retry");
        assertCode(response, ErrorCode.CONCURRENT_MODIFICATION, Map.of());
    }

    @Test
    void givenInvalidFields_whenValidationFails_thenReturns400WithTheCodeOfEachField() throws NoSuchMethodException {
        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(validationException(new Login(" ", " ")),
                new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

        ProblemDetail body = bodyOf(response);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getDetail()).isEqualTo("Validation failed");
        assertCode(body, ErrorCode.VALIDATION_FAILED, Map.of());
        assertThat(body.getProperties()).containsEntry(ProblemDetails.ERRORS, List.of(
                new ValidationErrorMapper.ValidationError("email", ErrorCode.VALIDATION_REQUIRED.key(), Map.of()),
                new ValidationErrorMapper.ValidationError("password", ErrorCode.VALIDATION_REQUIRED.key(), Map.of())));
    }

    @Test
    void givenWrongTypeInsideAList_whenTheBodyIsUnreadable_thenNamesTheFieldWithItsIndex() {
        ProblemDetail body = unreadableBodyProblem("""
                {"lines": [{"amount": 1}, {"amount": "many"}]}
                """);

        assertThat(body.getDetail()).isEqualTo("Invalid value for field 'lines[1].amount'");
        assertCode(body, ErrorCode.INVALID_VALUE, Map.of("field", "lines[1].amount"));
    }

    @Test
    void givenWrongTypeInANestedObject_whenTheBodyIsUnreadable_thenNamesTheFullPath() {
        ProblemDetail body = unreadableBodyProblem("""
                {"lines": [], "delivery": {"floor": "top"}}
                """);

        assertThat(body.getDetail()).isEqualTo("Invalid value for field 'delivery.floor'");
        assertCode(body, ErrorCode.INVALID_VALUE, Map.of("field", "delivery.floor"));
    }

    @Test
    void givenABodyThatIsNotJson_whenTheBodyIsUnreadable_thenReturnsTheMalformedBodyCode() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException("Unreadable",
                new MockHttpInputMessage("not json".getBytes(StandardCharsets.UTF_8)));

        ProblemDetail body = bodyOf(handler.handleHttpMessageNotReadable(exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, request));

        assertThat(body.getDetail()).isEqualTo("Request body is missing or malformed");
        assertCode(body, ErrorCode.MALFORMED_BODY, Map.of());
    }

    @Test
    void givenAPathVariableOfTheWrongType_whenHandled_thenNamesTheParameter() throws Exception {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException("last", UUID.class, "id",
                new MethodParameter(getClass().getDeclaredMethod("someEndpoint", Object.class), 0), new IllegalArgumentException());

        ResponseEntity<Object> response = handler.handleException(exception, request);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertCode(bodyOf(response), ErrorCode.INVALID_PARAMETER, Map.of("parameter", "id"));
    }

    @Test
    void givenAnUnknownRoute_whenHandled_thenReturnsTheRouteNotFoundCode() throws Exception {
        ResponseEntity<Object> response = handler.handleException(new NoResourceFoundException(HttpMethod.GET, "/unknown", "unknown"), request);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertCode(bodyOf(response), ErrorCode.ROUTE_NOT_FOUND, Map.of());
    }

    @Test
    void givenAMethodTheRouteDoesNotSupport_whenHandled_thenReturnsTheMethodNotAllowedCode() throws Exception {
        ResponseEntity<Object> response = handler.handleException(new HttpRequestMethodNotSupportedException("DELETE"), request);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertCode(bodyOf(response), ErrorCode.METHOD_NOT_ALLOWED, Map.of());
    }

    private static ProblemDetail bodyOf(ResponseEntity<Object> response) {
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
        return (ProblemDetail) response.getBody();
    }

    private ProblemDetail unreadableBodyProblem(String json) {
        JacksonException cause = catchThrowableOfType(JacksonException.class, () -> new JsonMapper().readValue(json, Order.class));
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException("Unreadable", cause,
                new MockHttpInputMessage(json.getBytes(StandardCharsets.UTF_8)));
        return bodyOf(handler.handleHttpMessageNotReadable(exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, request));
    }

    private MethodArgumentNotValidException validationException(Object target) throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "login");
        new SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().getValidator()).validate(target, bindingResult);
        MethodParameter parameter = new MethodParameter(getClass().getDeclaredMethod("someEndpoint", Object.class), 0);
        return new MethodArgumentNotValidException(parameter, bindingResult);
    }

    private record Login(@NotBlank String password, @NotBlank String email) {
    }

    private record Order(List<Line> lines, Delivery delivery) {
    }

    private record Line(Integer amount) {
    }

    private record Delivery(Integer floor) {
    }

    @SuppressWarnings("unused")
    private void someEndpoint(Object body) {
    }
}
