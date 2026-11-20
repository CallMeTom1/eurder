package com.tbaar.eurder.common;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

public final class EurderApi {

    public static final String BASE_PATH = "/api";

    private static final String CSRF_TOKEN = "csrf-token-of-the-tests";

    private EurderApi() {
    }

    public static RequestSpecification given(int port) {
        return givenWithoutCsrfToken(port)
                .cookie("XSRF-TOKEN", CSRF_TOKEN)
                .header("X-XSRF-TOKEN", CSRF_TOKEN);
    }

    public static RequestSpecification givenWithoutCsrfToken(int port) {
        return RestAssured.given().port(port).basePath(BASE_PATH);
    }
}
