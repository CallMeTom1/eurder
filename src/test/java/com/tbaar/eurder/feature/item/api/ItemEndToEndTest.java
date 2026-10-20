package com.tbaar.eurder.feature.item.api;

import com.tbaar.eurder.feature.customer.domain.CustomerRepository;
import com.tbaar.eurder.feature.item.domain.ItemRepository;
import com.tbaar.eurder.feature.security.domain.CredentialsRepository;
import com.tbaar.eurder.feature.security.domain.Role;
import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ItemEndToEndTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @AfterEach
    void deleteCreatedData() {
        itemRepository.deleteAll();
        customerRepository.deleteAll();
        credentialsRepository.deleteAllByRole(Role.CUSTOMER);
    }

    @Test
    void givenValidItem_whenTheAdminPostsIt_thenReturns201WithLocationAndTheItemWithATwoDecimalPrice() {
        RequestSpecification request = asAdmin(item("Chair", "49.9", "10"));

        Response response = request.post("/items");

        String id = response.then()
                .statusCode(HttpStatus.CREATED.value())
                .body("name", equalTo("Chair"))
                .body("description", equalTo("Wooden chair"))
                .body("price", equalTo(new BigDecimal("49.90")))
                .body("stock", equalTo(10))
                .body("keySet()", containsInAnyOrder("id", "name", "description", "price", "stock"))
                .extract().path("id");
        response.then().header(HttpHeaders.LOCATION, endsWith("/items/" + id));
    }

    @Test
    void givenItemOutOfStock_whenTheAdminPostsIt_thenReturns201() {
        RequestSpecification request = asAdmin(item("Chair", "49.99", "0"));

        Response response = request.post("/items");

        response.then()
                .statusCode(HttpStatus.CREATED.value())
                .body("stock", equalTo(0));
    }

    @Test
    void givenTheNameOfAnExistingItem_whenTheAdminPostsIt_thenBothItemsExist() {
        asAdmin(item("Chair", "49.99", "10")).post("/items");
        RequestSpecification request = asAdmin(item("Chair", "19.99", "5"));

        Response response = request.post("/items");

        response.then().statusCode(HttpStatus.CREATED.value());
        assertThat(itemRepository.count()).isEqualTo(2);
    }

    @Test
    void givenSeveralInvalidFields_whenTheAdminPostsAnItem_thenReturns400ListingEachFieldWithItsMessage() {
        RequestSpecification request = asAdmin("""
                {"name": " ", "description": "Wooden chair", "price": 0, "stock": -1}
                """);

        Response response = request.post("/items");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", equalTo("Validation failed"))
                .body("errors.keySet()", containsInAnyOrder("name", "price", "stock"))
                .body("errors.name", contains("Name is required"))
                .body("errors.price", contains("Price must be greater than 0"))
                .body("errors.stock", contains("Stock must be 0 or more"));
    }

    @Test
    void givenPriceWithThreeDecimals_whenTheAdminPostsAnItem_thenReturns400InsteadOfRounding() {
        RequestSpecification request = asAdmin(item("Chair", "9.999", "10"));

        Response response = request.post("/items");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("errors.price", contains("Price must have at most 8 digits before and 2 after the decimal point"));
        assertThat(itemRepository.count()).isZero();
    }

    @Test
    void givenMissingPriceAndStock_whenTheAdminPostsAnItem_thenReturns400NamingBoth() {
        RequestSpecification request = asAdmin("""
                {"name": "Chair", "description": "Wooden chair"}
                """);

        Response response = request.post("/items");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("errors.price", contains("Price is required"))
                .body("errors.stock", contains("Stock is required"));
    }

    @Test
    void givenPriceSentAsText_whenTheAdminPostsAnItem_thenReturns400NamingTheField() {
        RequestSpecification request = asAdmin(item("Chair", "\"cheap\"", "10"));

        Response response = request.post("/items");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", equalTo("Invalid value for field 'price'"));
    }

    @Test
    void givenACustomer_whenPostingAnItem_thenReturns403AndStoresNothing() {
        RestAssured.given().port(port).contentType(ContentType.JSON).body("""
                {"firstName": "Jane", "lastName": "Doe", "email": "jane@doe.be", "password": "secret123",
                 "address": {"street": "Rue Haute", "houseNumber": "1", "postalCode": "1000", "city": "Bruxelles"},
                 "phoneNumber": "0470123456"}
                """).post("/customers");
        RequestSpecification request = authenticatedAs("jane@doe.be", "secret123", item("Chair", "49.99", "10"));

        Response response = request.post("/items");

        response.then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("detail", equalTo("User jane@doe.be has no access to MANAGE_ITEMS"));
        assertThat(itemRepository.count()).isZero();
    }

    @Test
    void givenNoAuthorizationHeader_whenPostingAnItem_thenReturns401WithABasicChallenge() {
        RequestSpecification request = RestAssured.given().port(port).contentType(ContentType.JSON).body(item("Chair", "49.99", "10"));

        Response response = request.post("/items");

        response.then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Eurder\"");
        assertThat(itemRepository.count()).isZero();
    }

    private RequestSpecification asAdmin(String body) {
        return authenticatedAs("admin@eurder.be", "admin", body);
    }

    private RequestSpecification authenticatedAs(String email, String password, String body) {
        return RestAssured.given()
                .port(port)
                .config(RestAssured.config().jsonConfig(JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL)))
                .auth().preemptive().basic(email, password)
                .contentType(ContentType.JSON)
                .body(body);
    }

    private static String item(String name, String price, String stock) {
        return """
                {"name": "%s", "description": "Wooden chair", "price": %s, "stock": %s}
                """.formatted(name, price, stock);
    }
}
