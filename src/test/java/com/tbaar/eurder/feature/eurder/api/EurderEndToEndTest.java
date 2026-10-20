package com.tbaar.eurder.feature.eurder.api;

import com.tbaar.eurder.common.MutableClock;
import com.tbaar.eurder.feature.customer.domain.CustomerRepository;
import com.tbaar.eurder.feature.eurder.domain.EurderRepository;
import com.tbaar.eurder.feature.item.domain.AvailableUnits;
import com.tbaar.eurder.feature.item.domain.ItemRepository;
import com.tbaar.eurder.feature.item.domain.ItemUnitRepository;
import com.tbaar.eurder.feature.security.domain.CredentialsRepository;
import com.tbaar.eurder.feature.security.domain.Role;
import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EurderEndToEndTest {

    private static final ZonedDateTime NEW_YEARS_EVE = ZonedDateTime.of(2026, 12, 31, 10, 0, 0, 0, ZoneId.of("Europe/Brussels"));

    @TestConfiguration
    static class MutableClockConfig {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(NEW_YEARS_EVE);
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private MutableClock clock;

    @Autowired
    private EurderRepository eurderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemUnitRepository itemUnitRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @BeforeEach
    void registerJane() {
        clock.setTo(NEW_YEARS_EVE);
        registerCustomer("jane@doe.be");
    }

    @AfterEach
    void deleteCreatedData() {
        eurderRepository.deleteAll();
        itemUnitRepository.deleteAll();
        itemRepository.deleteAll();
        customerRepository.deleteAll();
        credentialsRepository.deleteAllByRole(Role.CUSTOMER);
    }

    @Test
    void givenItemsInAndOutOfStock_whenACustomerOrdersThem_thenReturns201WithShippingDatesSubtotalsAndTotal() {
        String chairId = createItem("Chair", "49.99", 10);
        String tableId = createItem("Table", "199.00", 0);
        RequestSpecification request = asJane("""
                {"itemGroups": [{"itemId": "%s", "amount": 3}, {"itemId": "%s", "amount": 1}]}
                """.formatted(chairId, tableId));

        Response response = request.post("/eurders");

        String id = response.then()
                .statusCode(HttpStatus.CREATED.value())
                .body("keySet()", containsInAnyOrder("id", "itemGroups", "totalPrice"))
                .body("itemGroups.itemId", contains(chairId, tableId))
                .body("itemGroups.itemName", contains("Chair", "Table"))
                .body("itemGroups.itemPrice", contains(new BigDecimal("49.99"), new BigDecimal("199.00")))
                .body("itemGroups.amount", contains(3, 1))
                .body("itemGroups.subtotal", contains(new BigDecimal("149.97"), new BigDecimal("199.00")))
                .body("itemGroups.shippingDate", contains("2027-01-01", "2027-01-07"))
                .body("totalPrice", equalTo(new BigDecimal("348.97")))
                .extract().path("id");
        response.then().header(HttpHeaders.LOCATION, endsWith("/eurders/" + id));
        assertThat(availableUnits(chairId)).isEqualTo(7);
    }

    @Test
    void givenAnUnknownItem_whenACustomerOrders_thenReturns404AndNoStockMoves() {
        String chairId = createItem("Chair", "49.99", 10);
        String unknownId = "3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90";
        RequestSpecification request = asJane("""
                {"itemGroups": [{"itemId": "%s", "amount": 3}, {"itemId": "%s", "amount": 1}]}
                """.formatted(chairId, unknownId));

        Response response = request.post("/eurders");

        response.then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("detail", equalTo("No item found for id " + unknownId));
        assertThat(eurderRepository.count()).isZero();
        assertThat(availableUnits(chairId)).isEqualTo(10);
    }

    @Test
    void givenNoItemGroup_whenACustomerOrders_thenReturns400() {
        RequestSpecification request = asJane("""
                {"itemGroups": []}
                """);

        Response response = request.post("/eurders");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("errors.itemGroups", contains("At least one item group is required"));
    }

    @Test
    void givenInvalidItemGroups_whenACustomerOrders_thenReturns400NamingEachItemGroupByItsIndex() {
        RequestSpecification request = asJane("""
                {"itemGroups": [{"itemId": "3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90", "amount": 1}, {"amount": 0}, null]}
                """);

        Response response = request.post("/eurders");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("errors.keySet()", containsInAnyOrder("itemGroups[1].itemId", "itemGroups[1].amount", "itemGroups[2]"))
                .body("errors.'itemGroups[1].itemId'", contains("Item id is required"))
                .body("errors.'itemGroups[1].amount'", contains("Amount must be greater than 0"))
                .body("errors.'itemGroups[2]'", contains("Item group is required"));
    }

    @Test
    void givenAmountSentAsText_whenACustomerOrders_thenReturns400NamingTheFieldWithItsIndex() {
        RequestSpecification request = asJane("""
                {"itemGroups": [{"itemId": "3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90", "amount": "many"}]}
                """);

        Response response = request.post("/eurders");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", equalTo("Invalid value for field 'itemGroups[0].amount'"));
    }

    @Test
    void givenItemIdThatIsNotAUuid_whenACustomerOrders_thenReturns400NamingTheField() {
        RequestSpecification request = asJane("""
                {"itemGroups": [{"itemId": "chair", "amount": 1}]}
                """);

        Response response = request.post("/eurders");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", equalTo("Invalid value for field 'itemGroups[0].itemId'"));
    }

    @Test
    void givenTheAdmin_whenOrdering_thenReturns403() {
        String chairId = createItem("Chair", "49.99", 10);
        RequestSpecification request = authenticatedAs("admin@eurder.be", "admin", """
                {"itemGroups": [{"itemId": "%s", "amount": 1}]}
                """.formatted(chairId));

        Response response = request.post("/eurders");

        response.then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("detail", equalTo("User admin@eurder.be has no access to ORDER_ITEMS"));
        assertThat(eurderRepository.count()).isZero();
    }

    @Test
    void givenNoAuthorizationHeader_whenOrdering_thenReturns401WithABasicChallenge() {
        String chairId = createItem("Chair", "49.99", 10);
        RequestSpecification request = RestAssured.given().port(port).contentType(ContentType.JSON).body("""
                {"itemGroups": [{"itemId": "%s", "amount": 1}]}
                """.formatted(chairId));

        Response response = request.post("/eurders");

        response.then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Eurder\"");
        assertThat(eurderRepository.count()).isZero();
    }

    @Test
    void givenTwoEurders_whenTheCustomerGetsTheReport_thenReturnsEachEurderWithItsLinesAndTheTotalOfAll() {
        String chairId = createItem("Chair", "49.99", 10);
        String lampId = createItem("Lamp", "0.10", 10);
        order("jane@doe.be", chairId, 3, lampId, 3);
        order("jane@doe.be", chairId, 1);
        RequestSpecification request = authenticatedAs("jane@doe.be", "secret123");

        Response response = request.get("/eurders/report");

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("keySet()", containsInAnyOrder("eurders", "totalPrice"))
                .body("eurders[0].keySet()", containsInAnyOrder("id", "itemGroups", "totalPrice"))
                .body("eurders[0].itemGroups[0].keySet()", containsInAnyOrder("itemName", "itemState", "amount", "subtotal"))
                .body("eurders.totalPrice", containsInAnyOrder(new BigDecimal("150.27"), new BigDecimal("49.99")))
                .body("eurders.itemGroups.flatten().subtotal",
                        containsInAnyOrder(new BigDecimal("149.97"), new BigDecimal("0.30"), new BigDecimal("49.99")))
                .body("totalPrice", equalTo(new BigDecimal("200.26")));
    }

    @Test
    void givenACustomerWithoutEurder_whenGettingTheReport_thenReturnsAnEmptyListAndAZeroTotal() {
        String chairId = createItem("Chair", "49.99", 10);
        registerCustomer("zoe@adams.be");
        order("zoe@adams.be", chairId, 2);
        RequestSpecification request = authenticatedAs("jane@doe.be", "secret123");

        Response response = request.get("/eurders/report");

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("eurders.size()", equalTo(0))
                .body("totalPrice", equalTo(new BigDecimal("0.00")));
    }

    @Test
    void givenTheAdmin_whenGettingTheReport_thenReturns403() {
        RequestSpecification request = authenticatedAs("admin@eurder.be", "admin");

        Response response = request.get("/eurders/report");

        response.then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("detail", equalTo("User admin@eurder.be has no access to VIEW_OWN_EURDERS"));
    }

    @Test
    void givenNoAuthorizationHeader_whenGettingTheReport_thenReturns401() {
        RequestSpecification request = RestAssured.given().port(port);

        Response response = request.get("/eurders/report");

        response.then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void givenAnEurderPlacedYesterday_whenTheAdminGetsWhatShipsToday_thenListsItsItemGroupInStockWithTheAddress() {
        String chairId = createItem("Chair", "49.99", 10);
        String tableId = createItem("Table", "199.00", 0);
        order("jane@doe.be", chairId, 3, tableId, 1);
        clock.setTo(NEW_YEARS_EVE.plusDays(1));
        RequestSpecification request = authenticatedAs("admin@eurder.be", "admin");

        Response response = request.get("/eurders/shipping-today");

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(1))
                .body("[0].keySet()", containsInAnyOrder("eurderId", "customerName", "address", "itemGroups"))
                .body("[0].customerName", equalTo("Jane Doe"))
                .body("[0].address.street", equalTo("Rue Haute"))
                .body("[0].address.city", equalTo("Bruxelles"))
                .body("[0].itemGroups.itemName", contains("Chair"))
                .body("[0].itemGroups.amount", contains(3))
                .body("[0].itemGroups.itemState", contains("PRISTINE"));
    }

    @Test
    void givenNothingShippingToday_whenTheAdminGetsWhatShipsToday_thenReturnsAnEmptyList() {
        String chairId = createItem("Chair", "49.99", 10);
        order("jane@doe.be", chairId, 1);
        RequestSpecification request = authenticatedAs("admin@eurder.be", "admin");

        Response response = request.get("/eurders/shipping-today");

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(0));
    }

    @Test
    void givenACustomer_whenGettingWhatShipsToday_thenReturns403() {
        RequestSpecification request = authenticatedAs("jane@doe.be", "secret123");

        Response response = request.get("/eurders/shipping-today");

        response.then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("detail", equalTo("User jane@doe.be has no access to VIEW_SHIPPING_TODAY"));
    }

    @Test
    void givenNoAuthorizationHeader_whenGettingWhatShipsToday_thenReturns401() {
        RequestSpecification request = RestAssured.given().port(port);

        Response response = request.get("/eurders/shipping-today");

        response.then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void givenOwnEurderAndALaterPriceChange_whenTheCustomerReordersIt_thenReturns201WithANewEurderAtTheCurrentPrice() {
        String chairId = createItem("Chair", "49.99", 10);
        String originalId = order("jane@doe.be", chairId, 3);
        authenticatedAs("admin@eurder.be", "admin", """
                {"name": "Chair", "description": "Chair description", "price": 59.99}
                """).put("/items/" + chairId).then().statusCode(HttpStatus.OK.value());
        RequestSpecification request = authenticatedAs("jane@doe.be", "secret123");

        Response response = request.post("/eurders/" + originalId + "/reorder");

        String reorderedId = response.then()
                .statusCode(HttpStatus.CREATED.value())
                .body("itemGroups.itemId", contains(chairId))
                .body("itemGroups.itemPrice", contains(new BigDecimal("59.99")))
                .body("itemGroups.amount", contains(3))
                .body("totalPrice", equalTo(new BigDecimal("179.97")))
                .extract().path("id");
        assertThat(reorderedId).isNotEqualTo(originalId);
        response.then().header(HttpHeaders.LOCATION, endsWith("/eurders/" + reorderedId));
    }

    @Test
    void givenAnEurderOfAnotherCustomer_whenReorderingIt_thenReturns404AsIfItDidNotExist() {
        String chairId = createItem("Chair", "49.99", 10);
        registerCustomer("zoe@adams.be");
        String zoesId = order("zoe@adams.be", chairId, 2);
        RequestSpecification request = authenticatedAs("jane@doe.be", "secret123");

        Response response = request.post("/eurders/" + zoesId + "/reorder");

        response.then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("detail", equalTo("No eurder found for id " + zoesId));
        assertThat(eurderRepository.count()).isEqualTo(1);
    }

    @Test
    void givenAnUnknownEurderId_whenReordering_thenReturns404() {
        String unknownId = "3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90";
        RequestSpecification request = authenticatedAs("jane@doe.be", "secret123");

        Response response = request.post("/eurders/" + unknownId + "/reorder");

        response.then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("detail", equalTo("No eurder found for id " + unknownId));
    }

    @Test
    void givenAnIdThatIsNotAUuid_whenReordering_thenReturns400() {
        RequestSpecification request = authenticatedAs("jane@doe.be", "secret123");

        Response response = request.post("/eurders/last/reorder");

        response.then().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void givenTheAdmin_whenReordering_thenReturns403() {
        RequestSpecification request = authenticatedAs("admin@eurder.be", "admin");

        Response response = request.post("/eurders/3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90/reorder");

        response.then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("detail", equalTo("User admin@eurder.be has no access to ORDER_ITEMS"));
    }

    @Test
    void givenNoAuthorizationHeader_whenReordering_thenReturns401() {
        RequestSpecification request = RestAssured.given().port(port);

        Response response = request.post("/eurders/3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90/reorder");

        response.then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void givenADamagedState_whenACustomerOrders_thenTheItemGroupShowsTheStateAndTheDiscountedPrice() {
        String chairId = createItem("Chair", "49.99", 10);
        RequestSpecification request = asJane("""
                {"itemGroups": [{"itemId": "%s", "amount": 2, "state": "DAMAGED"}]}
                """.formatted(chairId));

        Response response = request.post("/eurders");

        response.then()
                .statusCode(HttpStatus.CREATED.value())
                .body("itemGroups[0].itemState", equalTo("DAMAGED"))
                .body("itemGroups[0].itemPrice", equalTo(new BigDecimal("44.99")))
                .body("itemGroups[0].shippingDate", equalTo("2027-01-07"))
                .body("totalPrice", equalTo(new BigDecimal("89.98")));
    }

    private long availableUnits(String itemId) {
        return itemUnitRepository.countAvailableUnits(List.of(UUID.fromString(itemId))).stream().mapToLong(AvailableUnits::count).sum();
    }

    private void registerCustomer(String email) {
        RestAssured.given().port(port).contentType(ContentType.JSON).body("""
                {"firstName": "Jane", "lastName": "Doe", "email": "%s", "password": "secret123",
                 "address": {"street": "Rue Haute", "houseNumber": "1", "postalCode": "1000", "city": "Bruxelles"},
                 "phoneNumber": "0470123456"}
                """.formatted(email)).post("/customers").then().statusCode(HttpStatus.CREATED.value());
    }

    private String order(String email, String itemId, int amount) {
        return authenticatedAs(email, "secret123", """
                {"itemGroups": [{"itemId": "%s", "amount": %d}]}
                """.formatted(itemId, amount))
                .post("/eurders").then().statusCode(HttpStatus.CREATED.value())
                .extract().path("id");
    }

    private void order(String email, String firstItemId, int firstAmount, String secondItemId, int secondAmount) {
        authenticatedAs(email, "secret123", """
                {"itemGroups": [{"itemId": "%s", "amount": %d}, {"itemId": "%s", "amount": %d}]}
                """.formatted(firstItemId, firstAmount, secondItemId, secondAmount))
                .post("/eurders").then().statusCode(HttpStatus.CREATED.value());
    }

    private String createItem(String name, String price, int stock) {
        return authenticatedAs("admin@eurder.be", "admin", """
                {"name": "%s", "description": "%s description", "price": %s, "units": {"PRISTINE": %d}}
                """.formatted(name, name, price, stock))
                .post("/items")
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().path("id");
    }

    private RequestSpecification asJane(String body) {
        return authenticatedAs("jane@doe.be", "secret123", body);
    }

    private RequestSpecification authenticatedAs(String email, String password, String body) {
        return authenticatedAs(email, password)
                .contentType(ContentType.JSON)
                .body(body);
    }

    private RequestSpecification authenticatedAs(String email, String password) {
        return RestAssured.given()
                .port(port)
                .config(RestAssured.config().jsonConfig(JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL)))
                .auth().preemptive().basic(email, password);
    }
}
