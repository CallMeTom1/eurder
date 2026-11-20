package com.tbaar.eurder.feature.security;

import com.tbaar.eurder.common.AccessTokens;
import com.tbaar.eurder.common.EurderApi;
import com.tbaar.eurder.common.UniqueEmail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CsrfEndToEndTest {

    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String ADMIN = "admin@eurder.be";
    private static final String ADMIN_PASSWORD = "admin";

    @LocalServerPort
    private int port;

    private static String newAdmin() {
        return """
                {"email": "%s", "password": "secret123"}
                """.formatted(UniqueEmail.of("admin"));
    }

    @Test
    void givenNoCsrfToken_whenPosting_thenReturns403WithTheCsrfCode() {
        RequestSpecification request = EurderApi.givenWithoutCsrfToken(port);

        Response response = request.post("/auth/logout");

        response.then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("code", equalTo("error.security.invalid-csrf-token"));
    }

    @Test
    void givenAHeaderThatDoesNotMatchTheCookie_whenPosting_thenReturns403WithTheCsrfCode() {
        RequestSpecification request = EurderApi.givenWithoutCsrfToken(port)
                .cookie(CSRF_COOKIE, "the-cookie-value")
                .header(CSRF_HEADER, "another-value");

        Response response = request.post("/auth/logout");

        response.then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("code", equalTo("error.security.invalid-csrf-token"));
    }

    @Test
    void givenAHeaderMatchingTheCookie_whenPosting_thenTheRequestGoesThrough() {
        RequestSpecification request = EurderApi.givenWithoutCsrfToken(port)
                .cookie(CSRF_COOKIE, "the-same-value")
                .header(CSRF_HEADER, "the-same-value");

        Response response = request.post("/auth/logout");

        response.then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void givenTheAccessTokenCookieButNoCsrfToken_whenPosting_thenReturns403WithTheCsrfCode() {
        RequestSpecification request = EurderApi.givenWithoutCsrfToken(port)
                .cookie(ACCESS_TOKEN, AccessTokens.of(port, ADMIN, ADMIN_PASSWORD))
                .contentType(ContentType.JSON)
                .body(newAdmin());

        Response response = request.post("/administrators");

        response.then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("code", equalTo("error.security.invalid-csrf-token"));
    }

    @Test
    void givenTheAccessTokenCookieAndACsrfToken_whenPosting_thenTheRequestGoesThrough() {
        RequestSpecification request = EurderApi.given(port)
                .cookie(ACCESS_TOKEN, AccessTokens.of(port, ADMIN, ADMIN_PASSWORD))
                .contentType(ContentType.JSON)
                .body(newAdmin());

        Response response = request.post("/administrators");

        response.then().statusCode(HttpStatus.CREATED.value());
    }

    @Test
    void givenABearerHeaderWithoutCsrfToken_whenPosting_thenTheRequestGoesThroughSinceABrowserCannotForgeThatHeader() {
        RequestSpecification request = EurderApi.givenWithoutCsrfToken(port)
                .auth().oauth2(AccessTokens.of(port, ADMIN, ADMIN_PASSWORD))
                .contentType(ContentType.JSON)
                .body(newAdmin());

        Response response = request.post("/administrators");

        response.then().statusCode(HttpStatus.CREATED.value());
    }

    @Test
    void givenAReadRequest_whenSentWithoutCsrfToken_thenNoCsrfTokenIsRequired() {
        RequestSpecification request = EurderApi.givenWithoutCsrfToken(port).cookie(ACCESS_TOKEN, AccessTokens.of(port, ADMIN, ADMIN_PASSWORD));

        Response response = request.get("/customers");

        response.then().statusCode(HttpStatus.OK.value());
    }

    @Test
    void givenAnAnonymousVisitor_whenTheApiAnswers_thenSetsACsrfCookieThatThePagesOfTheWholeSiteCanRead() {
        RequestSpecification request = EurderApi.givenWithoutCsrfToken(port);

        Response response = request.get("/auth/me");

        response.then().statusCode(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getDetailedCookie(CSRF_COOKIE)).satisfies(cookie -> {
            assertThat(cookie.getValue()).isNotBlank();
            assertThat(cookie.getPath()).isEqualTo("/");
            assertThat(cookie.isHttpOnly()).isFalse();
            assertThat(cookie.isSecured()).isTrue();
            assertThat(cookie.getSameSite()).isEqualTo("Strict");
        });
    }
}
