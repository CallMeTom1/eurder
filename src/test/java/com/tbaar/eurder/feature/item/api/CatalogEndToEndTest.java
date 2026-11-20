package com.tbaar.eurder.feature.item.api;

import com.tbaar.eurder.common.AccessTokens;
import com.tbaar.eurder.common.EurderApi;
import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogEndToEndTest {

    @LocalServerPort
    private int port;

    private final String suffix = UUID.randomUUID().toString();

    @Test
    void givenAnItem_whenAnAnonymousVisitorReadsTheCatalog_thenListsItWithoutAnyStockInformation() {
        String chairId = createItem("Chair " + suffix, "49.99", 3);

        Response response = anonymous().get("/catalog");

        response.then().statusCode(HttpStatus.OK.value());
        Map<String, Object> chair = itemsOfThisTest(response).stream()
                .filter(item -> item.get("id").equals(chairId))
                .findFirst().orElseThrow();
        assertThat(chair).containsOnlyKeys("id", "name", "description", "price")
                .containsEntry("name", "Chair " + suffix)
                .containsEntry("description", "Wooden chair")
                .containsEntry("price", new BigDecimal("49.99"));
    }

    @Test
    void givenSeveralItems_whenReadingTheCatalog_thenTheyAreSortedByName() {
        createItem("Table " + suffix, "120.00", 1);
        createItem("Armchair " + suffix, "250.00", 2);
        createItem("Lamp " + suffix, "35.00", 8);

        Response response = anonymous().get("/catalog");

        assertThat(itemsOfThisTest(response)).extracting(item -> item.get("name"))
                .containsExactly("Armchair " + suffix, "Lamp " + suffix, "Table " + suffix);
    }

    @Test
    void givenAnItem_whenAnAnonymousVisitorReadsItsDetail_thenReturnsItWithoutAnyStockInformation() {
        String lampId = createItem("Lamp " + suffix, "35.00", 8);

        Response response = anonymous().get("/catalog/{id}", lampId);

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(lampId))
                .body("name", equalTo("Lamp " + suffix))
                .body("price", equalTo(new BigDecimal("35.00")));
        assertThat(response.jsonPath().getMap("$")).containsOnlyKeys("id", "name", "description", "price");
    }

    @Test
    void givenAnUnknownId_whenReadingTheDetail_thenReturns404WithTheItemCode() {
        UUID unknownId = UUID.randomUUID();

        Response response = anonymous().get("/catalog/{id}", unknownId);

        response.then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo("error.item.not-found"))
                .body("params.id", equalTo(unknownId.toString()));
    }

    private List<Map<String, Object>> itemsOfThisTest(Response response) {
        return response.jsonPath().<Map<String, Object>>getList("$").stream()
                .filter(item -> item.get("name").toString().endsWith(suffix))
                .toList();
    }

    private RequestSpecification anonymous() {
        return EurderApi.given(port)
                .config(RestAssured.config().jsonConfig(JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL)));
    }

    private String createItem(String name, String price, int pristineUnits) {
        return EurderApi.given(port)
                .auth().oauth2(AccessTokens.of(port, "admin@eurder.be", "admin"))
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "%s", "description": "Wooden chair", "price": %s, "units": {"PRISTINE": %d}}
                        """.formatted(name, price, pristineUnits))
                .post("/items")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .path("id");
    }
}
