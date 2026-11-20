package com.tbaar.eurder.common;

import io.restassured.http.ContentType;
import org.springframework.http.HttpStatus;

public final class AccessTokens {

    private AccessTokens() {
    }

    public static String of(int port, String email, String password) {
        return EurderApi.given(port)
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
