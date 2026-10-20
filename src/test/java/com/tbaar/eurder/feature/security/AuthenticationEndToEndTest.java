package com.tbaar.eurder.feature.security;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthenticationEndToEndTest {

    @LocalServerPort
    private int port;

    private final String email = "jane." + UUID.randomUUID() + "@doe.be";

    @ParameterizedTest(name = "Authorization: \"{0}\" is rejected")
    @ValueSource(strings = {"", "Bearer some-token", "Basic !!!not-base64!!!", "Basic bm8tY29sb24="})
    void givenAMissingOrMalformedBasicHeader_whenCallingAProtectedEndpoint_thenReturns401WithABasicChallenge(String authorization) {
        RequestSpecification request = RestAssured.given().port(port).header(HttpHeaders.AUTHORIZATION, authorization);

        Response response = request.get("/customers");

        response.then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Eurder\"")
                .body("detail", equalTo("Invalid credentials"));
    }

    @Test
    void givenAnUnknownEmail_whenCallingAProtectedEndpoint_thenReturnsTheSame401AsAWrongPassword() {
        RequestSpecification request = RestAssured.given().port(port).auth().preemptive().basic("nobody@eurder.be", "admin");

        Response response = request.get("/customers");

        response.then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("detail", equalTo("Invalid credentials"));
    }

    @Test
    void givenTheAdminEmailWithCapitalsAndSpaces_whenCallingAnAdminEndpoint_thenAuthenticatesTheAdmin() {
        RequestSpecification request = RestAssured.given().port(port).auth().preemptive().basic(" Admin@Eurder.BE ", "admin");

        Response response = request.get("/customers");

        response.then().statusCode(HttpStatus.OK.value());
    }

    @Test
    void givenAPasswordContainingColons_whenTheCustomerAuthenticates_thenEverythingAfterTheFirstColonIsThePassword() {
        RestAssured.given().port(port).contentType(ContentType.JSON).body("""
                {"firstName": "Jane", "lastName": "Doe", "email": "%s", "password": "se:cret:123",
                 "address": {"street": "Rue Haute", "houseNumber": "1", "postalCode": "1000", "city": "Bruxelles"},
                 "phoneNumber": "0470123456"}
                """.formatted(email)).post("/customers").then().statusCode(HttpStatus.CREATED.value());
        RequestSpecification request = RestAssured.given().port(port).auth().preemptive().basic(email, "se:cret:123");

        Response response = request.get("/eurders/report");

        response.then().statusCode(HttpStatus.OK.value());
    }

    @Test
    void givenTheAdmin_whenCallingACustomerEndpoint_thenReturns403NamingTheMissingFeature() {
        RequestSpecification request = RestAssured.given().port(port).auth().preemptive().basic("admin@eurder.be", "admin");

        Response response = request.get("/eurders/report");

        response.then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("detail", equalTo("User admin@eurder.be has no access to VIEW_OWN_EURDERS"));
    }
}
