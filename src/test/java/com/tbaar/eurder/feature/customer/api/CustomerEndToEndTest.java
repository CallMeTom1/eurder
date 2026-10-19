package com.tbaar.eurder.feature.customer.api;

import com.tbaar.eurder.feature.customer.domain.CustomerRepository;
import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.CredentialsRepository;
import com.tbaar.eurder.feature.security.domain.Role;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerEndToEndTest {

    @LocalServerPort
    private int port;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @AfterEach
    void deleteRegisteredCustomers() {
        customerRepository.deleteAll();
        credentialsRepository.deleteAllByRole(Role.CUSTOMER);
    }

    @Test
    void givenValidRegistration_whenPostingCustomer_thenReturns201WithLocationAndTheCustomerWithoutPassword() {
        RequestSpecification request = jsonRequest(registration("Jane@Doe.be", "secret123", "Bruxelles"));

        Response response = request.post("/customers");

        String id = response.then()
                .statusCode(HttpStatus.CREATED.value())
                .body("email", equalTo("jane@doe.be"))
                .body("firstName", equalTo("Jane"))
                .body("address.city", equalTo("Bruxelles"))
                .body("keySet()", containsInAnyOrder("id", "firstName", "lastName", "email", "address", "phoneNumber"))
                .extract().path("id");
        response.then().header(HttpHeaders.LOCATION, endsWith("/customers/" + id));
    }

    @Test
    void givenEmailAlreadyUsedWithAnotherCase_whenPostingCustomer_thenReturns409() {
        jsonRequest(registration("jane@doe.be", "secret123", "Bruxelles")).post("/customers");
        RequestSpecification request = jsonRequest(registration("JANE@DOE.BE", "secret123", "Bruxelles"));

        Response response = request.post("/customers");

        response.then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("detail", equalTo("Email jane@doe.be is already in use"));
    }

    @Test
    void givenTheDefaultAdminEmail_whenPostingCustomer_thenReturns409() {
        RequestSpecification request = jsonRequest(registration("admin@eurder.be", "secret123", "Bruxelles"));

        Response response = request.post("/customers");

        response.then().statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    void givenSeveralInvalidFields_whenPostingCustomer_thenReturns400ListingEachFieldWithItsMessage() {
        RequestSpecification request = jsonRequest("""
                {"firstName": " ", "lastName": "Doe", "email": "jane", "password": "secret123",
                 "address": {"street": "Rue Haute", "houseNumber": "1", "postalCode": "1000", "city": ""},
                 "phoneNumber": "0470123456"}
                """);

        Response response = request.post("/customers");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", equalTo("Validation failed"))
                .body("errors.firstName", contains("First name is required"))
                .body("errors.email", contains("Email must be valid"))
                .body("errors.'address.city'", contains("City is required"));
    }

    @Test
    void givenBlankPassword_whenPostingCustomer_thenReportsEveryPasswordViolation() {
        RequestSpecification request = jsonRequest(registration("jane@doe.be", "", "Bruxelles"));

        Response response = request.post("/customers");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("errors.password", contains("Password is required", "Password must be between 8 and 72 characters"));
    }

    @Test
    void givenRoleAdminInTheRequest_whenPostingCustomer_thenTheAccountIsStillACustomer() {
        RequestSpecification request = jsonRequest("""
                {"firstName": "Jane", "lastName": "Doe", "email": "jane@doe.be", "password": "secret123",
                 "address": {"street": "Rue Haute", "houseNumber": "1", "postalCode": "1000", "city": "Bruxelles"},
                 "phoneNumber": "0470123456", "role": "ADMIN"}
                """);

        Response response = request.post("/customers");

        response.then().statusCode(HttpStatus.CREATED.value());
        Optional<Credentials> credentials = credentialsRepository.findByEmail("jane@doe.be");
        assertThat(credentials).hasValueSatisfying(created -> assertThat(created.getRole()).isEqualTo(Role.CUSTOMER));
    }

    private RequestSpecification jsonRequest(String body) {
        return RestAssured.given()
                .port(port)
                .contentType(ContentType.JSON)
                .body(body);
    }

    private static String registration(String email, String password, String city) {
        return """
                {"firstName": "Jane", "lastName": "Doe", "email": "%s", "password": "%s",
                 "address": {"street": "Rue Haute", "houseNumber": "1", "postalCode": "1000", "city": "%s"},
                 "phoneNumber": "0470123456"}
                """.formatted(email, password, city);
    }
}
