package com.tbaar.eurder.feature.security.api;

import com.tbaar.eurder.common.UniqueEmail;
import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.CredentialsRepository;
import com.tbaar.eurder.feature.security.domain.Role;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminEndToEndTest {

    private static final String DEFAULT_ADMIN_EMAIL = "admin@eurder.be";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin";
    private static final String PASSWORD = "secret123";

    @LocalServerPort
    private int port;

    @Autowired
    private CredentialsRepository credentialsRepository;

    private final String janeEmail = UniqueEmail.of("jane");

    @Test
    void givenValidRegistration_whenTheAdminPostsAnAdmin_thenReturns201WithoutPassword() {
        RequestSpecification request = asDefaultAdmin(registration(janeEmail.toUpperCase(Locale.ROOT), PASSWORD));

        Response response = request.post("/administrators");

        response.then()
                .statusCode(HttpStatus.CREATED.value())
                .body("email", equalTo(janeEmail))
                .body("keySet()", containsInAnyOrder("id", "email"));
    }

    @Test
    void givenValidRegistration_whenTheAdminPostsAnAdmin_thenTheAccountHasTheAdminRole() {
        RequestSpecification request = asDefaultAdmin(registration(janeEmail, PASSWORD));

        request.post("/administrators");

        Optional<Credentials> credentials = credentialsRepository.findByEmail(janeEmail);
        assertThat(credentials).hasValueSatisfying(created -> assertThat(created.getRole()).isEqualTo(Role.ADMIN));
    }

    @Test
    void givenANewAdmin_whenListingCustomers_thenReturns200WithoutThatAdmin() {
        createAdmin(janeEmail);
        RequestSpecification request = authenticatedAs(janeEmail, PASSWORD);

        Response response = request.get("/customers");

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("email", not(hasItem(janeEmail)));
    }

    @Test
    void givenANewAdmin_whenPostingAnAdmin_thenReturns201() {
        createAdmin(janeEmail);
        RequestSpecification request = authenticatedAs(janeEmail, PASSWORD)
                .contentType(ContentType.JSON)
                .body(registration(UniqueEmail.of("zoe"), PASSWORD));

        Response response = request.post("/administrators");

        response.then().statusCode(HttpStatus.CREATED.value());
    }

    @Test
    void givenANewAdmin_whenOrderingItems_thenReturns403() {
        createAdmin(janeEmail);
        RequestSpecification request = authenticatedAs(janeEmail, PASSWORD)
                .contentType(ContentType.JSON)
                .body("""
                        {"itemGroups": [{"itemId": "3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90", "amount": 1}]}
                        """);

        Response response = request.post("/eurders");

        response.then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void givenEmailAlreadyUsedWithAnotherCase_whenPostingAnAdmin_thenReturns409() {
        createAdmin(janeEmail);
        RequestSpecification request = asDefaultAdmin(registration(janeEmail.toUpperCase(Locale.ROOT), PASSWORD));

        Response response = request.post("/administrators");

        response.then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("detail", equalTo("Email " + janeEmail + " is already in use"));
    }

    @Test
    void givenTheDefaultAdminEmail_whenPostingAnAdmin_thenReturns409() {
        RequestSpecification request = asDefaultAdmin(registration(DEFAULT_ADMIN_EMAIL, PASSWORD));

        Response response = request.post("/administrators");

        response.then().statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    void givenACustomer_whenPostingAnAdmin_thenReturns403AndCreatesNothing() {
        String customerEmail = UniqueEmail.of("customer");
        registerCustomer(customerEmail);
        RequestSpecification request = authenticatedAs(customerEmail, PASSWORD)
                .contentType(ContentType.JSON)
                .body(registration(janeEmail, PASSWORD));

        Response response = request.post("/administrators");

        response.then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("detail", equalTo("User " + customerEmail + " has no access to REGISTER_ADMIN"));
        assertThat(credentialsRepository.findByEmail(janeEmail)).isEmpty();
    }

    @Test
    void givenNoAuthorizationHeader_whenPostingAnAdmin_thenReturns401WithABasicChallenge() {
        RequestSpecification request = RestAssured.given()
                .port(port)
                .contentType(ContentType.JSON)
                .body(registration(janeEmail, PASSWORD));

        Response response = request.post("/administrators");

        response.then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Eurder\"");
    }

    @Test
    void givenWrongAdminPassword_whenPostingAnAdmin_thenReturns401() {
        RequestSpecification request = authenticatedAs(DEFAULT_ADMIN_EMAIL, "not-admin")
                .contentType(ContentType.JSON)
                .body(registration(janeEmail, PASSWORD));

        Response response = request.post("/administrators");

        response.then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("detail", equalTo("Invalid credentials"));
    }

    @Test
    void givenInvalidEmail_whenPostingAnAdmin_thenReturns400NamingTheField() {
        RequestSpecification request = asDefaultAdmin(registration("jane", PASSWORD));

        Response response = request.post("/administrators");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", equalTo("Validation failed"))
                .body("errors.email", contains("Email must be valid"));
    }

    @Test
    void givenBlankPassword_whenPostingAnAdmin_thenReportsEveryPasswordViolation() {
        RequestSpecification request = asDefaultAdmin(registration(janeEmail, ""));

        Response response = request.post("/administrators");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("errors.password", contains("Password is required", "Password must be between 8 and 72 characters"));
    }

    @Test
    void givenMalformedJson_whenPostingAnAdmin_thenReturns400WithAGenericMessage() {
        RequestSpecification request = asDefaultAdmin("{\"email\": \"jane@doe.be\",");

        Response response = request.post("/administrators");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", equalTo("Request body is missing or malformed"));
    }

    @Test
    void givenNoBody_whenPostingAnAdmin_thenReturns400WithAGenericMessage() {
        RequestSpecification request = authenticatedAs(DEFAULT_ADMIN_EMAIL, DEFAULT_ADMIN_PASSWORD)
                .contentType(ContentType.JSON);

        Response response = request.post("/administrators");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", equalTo("Request body is missing or malformed"));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void givenTooShortPassword_whenPostingAnAdmin_thenThePasswordIsNotLogged(CapturedOutput output) {
        String tooShortPassword = "Xq7#zK";
        RequestSpecification request = asDefaultAdmin(registration(janeEmail, tooShortPassword));

        Response response = request.post("/administrators");

        response.then().statusCode(HttpStatus.BAD_REQUEST.value());
        assertThat(output.getAll()).doesNotContain(tooShortPassword);
    }

    private void createAdmin(String email) {
        asDefaultAdmin(registration(email, PASSWORD)).post("/administrators")
                .then().statusCode(HttpStatus.CREATED.value());
    }

    private void registerCustomer(String email) {
        RestAssured.given()
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {"firstName": "Jane", "lastName": "Doe", "email": "%s", "password": "%s",
                         "address": {"street": "Rue Haute", "houseNumber": "1", "postalCode": "1000", "city": "Bruxelles"},
                         "phoneNumber": "0470123456"}
                        """.formatted(email, PASSWORD))
                .post("/customers")
                .then().statusCode(HttpStatus.CREATED.value());
    }

    private RequestSpecification asDefaultAdmin(String body) {
        return authenticatedAs(DEFAULT_ADMIN_EMAIL, DEFAULT_ADMIN_PASSWORD)
                .contentType(ContentType.JSON)
                .body(body);
    }

    private RequestSpecification authenticatedAs(String email, String password) {
        return RestAssured.given()
                .port(port)
                .auth().preemptive().basic(email, password);
    }

    private static String registration(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }
}
