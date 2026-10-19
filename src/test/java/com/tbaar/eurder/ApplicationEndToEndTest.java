package com.tbaar.eurder;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationEndToEndTest {

    @LocalServerPort
    private int port;

    @Test
    void givenRunningApplication_whenRequestingOpenApiDocs_thenReturnsEurderDocumentation() {
        RequestSpecification request = RestAssured.given().port(port);

        Response response = request.get("/v3/api-docs");

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("info.title", equalTo("Eurder"));
    }

    @Test
    void givenRunningApplication_whenRequestingSwaggerUi_thenReturns200() {
        RequestSpecification request = RestAssured.given().port(port);

        Response response = request.get("/docs");

        response.then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void givenUnknownUrl_whenRequested_thenReturns404ProblemDetail() {
        RequestSpecification request = RestAssured.given().port(port);

        Response response = request.get("/does-not-exist");

        response.then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("status", equalTo(404));
    }
}
