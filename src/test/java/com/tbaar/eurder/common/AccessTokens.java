package com.tbaar.eurder.common;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.http.HttpStatus;

public final class AccessTokens {

    private AccessTokens() {
    }

    public static String of(int port, String email, String password) {
        return RestAssured.given()
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password))
                .post("/auth/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .path("accessToken");
    }
}
