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

    @Test
    void givenMalformedJson_whenPostingCustomer_thenReturns400WithAGenericMessage() {
        RequestSpecification request = jsonRequest("{\"firstName\": \"Jane\",");

        Response response = request.post("/customers");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", equalTo("Request body is missing or malformed"));
    }

    @Test
    void givenNoBody_whenPostingCustomer_thenReturns400WithAGenericMessage() {
        RequestSpecification request = RestAssured.given().port(port).contentType(ContentType.JSON);

        Response response = request.post("/customers");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", equalTo("Request body is missing or malformed"));
    }

    @Test
    void givenAddressSentAsText_whenPostingCustomer_thenReturns400NamingTheField() {
        RequestSpecification request = jsonRequest("""
                {"firstName": "Jane", "lastName": "Doe", "email": "jane@doe.be", "password": "secret123",
                 "address": "Bruxelles", "phoneNumber": "0470123456"}
                """);

        Response response = request.post("/customers");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", equalTo("Invalid value for field 'address'"));
    }

    @Test
    void givenNestedFieldWithTheWrongType_whenPostingCustomer_thenReturns400NamingTheFullPath() {
        RequestSpecification request = jsonRequest("""
                {"firstName": "Jane", "lastName": "Doe", "email": "jane@doe.be", "password": "secret123",
                 "address": {"street": "Rue Haute", "houseNumber": ["1", "2"], "postalCode": "1000", "city": "Bruxelles"},
                 "phoneNumber": "0470123456"}
                """);

        Response response = request.post("/customers");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", equalTo("Invalid value for field 'address.houseNumber'"));
    }

    @Test
    void givenTwoRegisteredCustomers_whenTheAdminListsCustomers_thenReturnsThemSortedWithoutPassword() {
        jsonRequest(registrationOf("jane@doe.be", "Jane", "Doe")).post("/customers");
        jsonRequest(registrationOf("zoe@adams.be", "Zoe", "Adams")).post("/customers");
        RequestSpecification request = authenticatedAs("admin@eurder.be", "admin");

        Response response = request.get("/customers");

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("lastName", contains("Adams", "Doe"))
                .body("email", contains("zoe@adams.be", "jane@doe.be"))
                .body("[0].keySet()", containsInAnyOrder("id", "firstName", "lastName", "email", "address", "phoneNumber"));
    }

    @Test
    void givenNoRegisteredCustomer_whenTheAdminListsCustomers_thenReturnsAnEmptyListWithoutTheAdmin() {
        RequestSpecification request = authenticatedAs("admin@eurder.be", "admin");

        Response response = request.get("/customers");

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(0));
    }

    @Test
    void givenACustomer_whenListingCustomers_thenReturns403() {
        jsonRequest(registrationOf("jane@doe.be", "Jane", "Doe")).post("/customers");
        RequestSpecification request = authenticatedAs("jane@doe.be", "secret123");

        Response response = request.get("/customers");

        response.then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("detail", equalTo("User jane@doe.be has no access to VIEW_CUSTOMERS"));
    }

    @Test
    void givenNoAuthorizationHeader_whenListingCustomers_thenReturns401WithABasicChallenge() {
        RequestSpecification request = RestAssured.given().port(port);

        Response response = request.get("/customers");

        response.then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Eurder\"");
    }

    @Test
    void givenWrongAdminPassword_whenListingCustomers_thenReturns401() {
        RequestSpecification request = authenticatedAs("admin@eurder.be", "not-admin");

        Response response = request.get("/customers");

        response.then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("detail", equalTo("Invalid credentials"));
    }

    @Test
    void givenRegisteredCustomer_whenTheAdminGetsItById_thenReturnsItsDetailsWithoutPassword() {
        String id = jsonRequest(registrationOf("jane@doe.be", "Jane", "Doe")).post("/customers").path("id");
        RequestSpecification request = authenticatedAs("admin@eurder.be", "admin");

        Response response = request.get("/customers/" + id);

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(id))
                .body("email", equalTo("jane@doe.be"))
                .body("address.city", equalTo("Bruxelles"))
                .body("keySet()", containsInAnyOrder("id", "firstName", "lastName", "email", "address", "phoneNumber"));
    }

    @Test
    void givenUnknownId_whenTheAdminGetsACustomer_thenReturns404WithTheId() {
        RequestSpecification request = authenticatedAs("admin@eurder.be", "admin");

        Response response = request.get("/customers/3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90");

        response.then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("detail", equalTo("No customer found for id 3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90"));
    }

    @Test
    void givenTheIdOfTheAdminCredentials_whenTheAdminGetsACustomer_thenReturns404() {
        RequestSpecification request = authenticatedAs("admin@eurder.be", "admin");

        Response response = request.get("/customers/9d92d67e-6ef3-450d-85ac-11180c8826d2");

        response.then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("detail", equalTo("No customer found for id 9d92d67e-6ef3-450d-85ac-11180c8826d2"));
    }

    @Test
    void givenACustomer_whenGettingACustomerById_thenReturns403() {
        String id = jsonRequest(registrationOf("jane@doe.be", "Jane", "Doe")).post("/customers").path("id");
        RequestSpecification request = authenticatedAs("jane@doe.be", "secret123");

        Response response = request.get("/customers/" + id);

        response.then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void givenNoAuthorizationHeader_whenGettingACustomerById_thenReturns401() {
        RequestSpecification request = RestAssured.given().port(port);

        Response response = request.get("/customers/3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90");

        response.then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    private RequestSpecification authenticatedAs(String email, String password) {
        return RestAssured.given()
                .port(port)
                .auth().preemptive().basic(email, password);
    }

    private static String registrationOf(String email, String firstName, String lastName) {
        return """
                {"firstName": "%s", "lastName": "%s", "email": "%s", "password": "secret123",
                 "address": {"street": "Rue Haute", "houseNumber": "1", "postalCode": "1000", "city": "Bruxelles"},
                 "phoneNumber": "0470123456"}
                """.formatted(firstName, lastName, email);
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
