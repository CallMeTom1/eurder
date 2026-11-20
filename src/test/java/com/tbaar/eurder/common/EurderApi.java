package com.tbaar.eurder.common;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

public final class EurderApi {

    public static final String BASE_PATH = "/api";

    private EurderApi() {
    }

    public static RequestSpecification given(int port) {
        return RestAssured.given().port(port).basePath(BASE_PATH);
    }
}
