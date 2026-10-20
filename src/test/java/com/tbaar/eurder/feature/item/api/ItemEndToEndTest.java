package com.tbaar.eurder.feature.item.api;

import com.tbaar.eurder.feature.customer.domain.CustomerRepository;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ItemEndToEndTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemUnitRepository itemUnitRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @AfterEach
    void deleteCreatedData() {
        itemUnitRepository.deleteAll();
        itemRepository.deleteAll();
        customerRepository.deleteAll();
        credentialsRepository.deleteAllByRole(Role.CUSTOMER);
    }

    @Test
    void givenValidItemWithUnitsInTwoStates_whenTheAdminPostsIt_thenReturns201WithLocationAndItsAvailableUnitsPerState() {
        RequestSpecification request = asAdmin("""
                {"name": "Chair", "description": "Wooden chair", "price": 49.9, "units": {"PRISTINE": 8, "DAMAGED": 2}}
                """);

        Response response = request.post("/items");

        String id = response.then()
                .statusCode(HttpStatus.CREATED.value())
                .body("name", equalTo("Chair"))
                .body("description", equalTo("Wooden chair"))
                .body("price", equalTo(new BigDecimal("49.90")))
                .body("availableUnits.PRISTINE", equalTo(8))
                .body("availableUnits.SLIGHTLY_DAMAGED", equalTo(0))
                .body("availableUnits.DAMAGED", equalTo(2))
                .body("keySet()", containsInAnyOrder("id", "name", "description", "price", "availableUnits"))
                .extract().path("id");
        response.then().header(HttpHeaders.LOCATION, endsWith("/items/" + id));
    }

    @Test
    void givenItemWithoutAnyUnit_whenTheAdminPostsIt_thenReturns201WithZeroUnitsInEveryState() {
        RequestSpecification request = asAdmin(newItem("Chair", "49.99", 0));

        Response response = request.post("/items");

        response.then()
                .statusCode(HttpStatus.CREATED.value())
                .body("availableUnits.values()", contains(0, 0, 0));
    }

    @Test
    void givenMoreThanAThousandUnitsOfAState_whenTheAdminPostsAnItem_thenReturns400NamingTheState() {
        RequestSpecification request = asAdmin(newItem("Chair", "49.99", 1001));

        Response response = request.post("/items");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("errors.'units[PRISTINE]'", contains("Units per state must be at most 1000"));
        assertThat(itemRepository.count()).isZero();
    }

    @Test
    void givenAnUnknownState_whenTheAdminPostsAnItem_thenReturns400NamingTheField() {
        RequestSpecification request = asAdmin("""
                {"name": "Chair", "description": "Wooden chair", "price": 49.99, "units": {"BROKEN": 1}}
                """);

        Response response = request.post("/items");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", startsWith("Invalid value for field 'units"));
    }

    @Test
    void givenTheNameOfAnExistingItem_whenTheAdminPostsIt_thenBothItemsExist() {
        asAdmin(newItem("Chair", "49.99", 10)).post("/items");
        RequestSpecification request = asAdmin(newItem("Chair", "19.99", 5));

        Response response = request.post("/items");

        response.then().statusCode(HttpStatus.CREATED.value());
        assertThat(itemRepository.count()).isEqualTo(2);
    }

    @Test
    void givenSeveralInvalidFields_whenTheAdminPostsAnItem_thenReturns400ListingEachFieldWithItsMessage() {
        RequestSpecification request = asAdmin("""
                {"name": " ", "description": "Wooden chair", "price": 0, "units": {"PRISTINE": -1}}
                """);

        Response response = request.post("/items");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", equalTo("Validation failed"))
                .body("errors.keySet()", containsInAnyOrder("name", "price", "units[PRISTINE]"))
                .body("errors.name", contains("Name is required"))
                .body("errors.price", contains("Price must be greater than 0"))
                .body("errors.'units[PRISTINE]'", contains("Units per state must be 0 or more"));
    }

    @Test
    void givenPriceWithThreeDecimals_whenTheAdminPostsAnItem_thenReturns400InsteadOfRounding() {
        RequestSpecification request = asAdmin(newItem("Chair", "9.999", 10));

        Response response = request.post("/items");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("errors.price", contains("Price must have at most 8 digits before and 2 after the decimal point"));
        assertThat(itemRepository.count()).isZero();
    }

    @Test
    void givenMissingPriceAndUnits_whenTheAdminPostsAnItem_thenReturns400NamingBoth() {
        RequestSpecification request = asAdmin("""
                {"name": "Chair", "description": "Wooden chair"}
                """);

        Response response = request.post("/items");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("errors.price", contains("Price is required"))
                .body("errors.units", contains("Units are required"));
    }

    @Test
    void givenPriceSentAsText_whenTheAdminPostsAnItem_thenReturns400NamingTheField() {
        RequestSpecification request = asAdmin(newItem("Chair", "\"cheap\"", 10));

        Response response = request.post("/items");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", equalTo("Invalid value for field 'price'"));
    }

    @Test
    void givenACustomer_whenPostingAnItem_thenReturns403AndStoresNothing() {
        registerJane();
        RequestSpecification request = authenticatedAs("jane@doe.be", "secret123", newItem("Chair", "49.99", 10));

        Response response = request.post("/items");

        response.then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("detail", equalTo("User jane@doe.be has no access to MANAGE_ITEMS"));
        assertThat(itemRepository.count()).isZero();
    }

    @Test
    void givenNoAuthorizationHeader_whenPostingAnItem_thenReturns401WithABasicChallenge() {
        RequestSpecification request = RestAssured.given().port(port).contentType(ContentType.JSON).body(newItem("Chair", "49.99", 10));

        Response response = request.post("/items");

        response.then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Eurder\"");
        assertThat(itemRepository.count()).isZero();
    }

    @Test
    void givenExistingItem_whenTheAdminPutsNewValues_thenReturns200WithTheUpdatedItem() {
        String id = createItem();
        RequestSpecification request = asAdmin(item("Armchair", "150", "3"));

        Response response = request.put("/items/" + id);

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(id))
                .body("name", equalTo("Armchair"))
                .body("price", equalTo(new BigDecimal("150.00")));
        assertThat(itemRepository.findById(UUID.fromString(id)).orElseThrow().getName()).isEqualTo("Armchair");
    }

    @Test
    void givenUnknownId_whenTheAdminPutsAnItem_thenReturns404WithTheId() {
        String unknownId = "3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90";
        RequestSpecification request = asAdmin(item("Armchair", "150", "3"));

        Response response = request.put("/items/" + unknownId);

        response.then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("detail", equalTo("No item found for id " + unknownId));
    }

    @Test
    void givenInvalidValues_whenTheAdminPutsAnItem_thenReturns400WithTheCreationRulesAndChangesNothing() {
        String id = createItem();
        RequestSpecification request = asAdmin("""
                {"name": "", "description": "Wooden chair", "price": 9.999, "stock": -1}
                """);

        Response response = request.put("/items/" + id);

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("errors.keySet()", containsInAnyOrder("name", "price", "stock"));
        assertThat(itemRepository.findById(UUID.fromString(id)).orElseThrow().getName()).isEqualTo("Chair");
    }

    @Test
    void givenAnIdThatIsNotAUuid_whenTheAdminPutsAnItem_thenReturns400() {
        RequestSpecification request = asAdmin(item("Armchair", "150", "3"));

        Response response = request.put("/items/chair");

        response.then().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void givenACustomer_whenPuttingAnItem_thenReturns403AndChangesNothing() {
        String id = createItem();
        registerJane();
        RequestSpecification request = authenticatedAs("jane@doe.be", "secret123", item("Armchair", "150", "3"));

        Response response = request.put("/items/" + id);

        response.then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("detail", equalTo("User jane@doe.be has no access to MANAGE_ITEMS"));
        assertThat(itemRepository.findById(UUID.fromString(id)).orElseThrow().getName()).isEqualTo("Chair");
    }

    @Test
    void givenNoAuthorizationHeader_whenPuttingAnItem_thenReturns401() {
        String id = createItem();
        RequestSpecification request = RestAssured.given().port(port).contentType(ContentType.JSON).body(item("Armchair", "150", "3"));

        Response response = request.put("/items/" + id);

        response.then().statusCode(HttpStatus.UNAUTHORIZED.value());
        assertThat(itemRepository.findById(UUID.fromString(id)).orElseThrow().getName()).isEqualTo("Chair");
    }

    @Test
    void givenItemsWithDifferentStocks_whenTheAdminGetsTheOverview_thenReturnsThemFromMostToLeastUrgent() {
        createItem("Table", 12);
        createItem("Lamp", 0);
        createItem("Chair", 7);
        RequestSpecification request = authenticatedAs("admin@eurder.be", "admin");

        Response response = request.get("/items");

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("name", contains("Lamp", "Chair", "Table"))
                .body("urgency", contains("STOCK_LOW", "STOCK_MEDIUM", "STOCK_HIGH"))
                .body("[0].keySet()", containsInAnyOrder("id", "name", "description", "price", "availableUnits", "urgency"));
    }

    @Test
    void givenAnUrgencyFilter_whenTheAdminGetsTheOverview_thenReturnsOnlyTheItemsOfThatUrgency() {
        createItem("Table", 12);
        createItem("Lamp", 0);
        createItem("Desk", 4);
        RequestSpecification request = authenticatedAs("admin@eurder.be", "admin").queryParam("urgency", "STOCK_LOW");

        Response response = request.get("/items");

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("name", contains("Lamp", "Desk"));
    }

    @Test
    void givenNoItem_whenTheAdminGetsTheOverview_thenReturnsAnEmptyList() {
        RequestSpecification request = authenticatedAs("admin@eurder.be", "admin");

        Response response = request.get("/items");

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(0));
    }

    @ParameterizedTest(name = "urgency \"{0}\" is rejected")
    @ValueSource(strings = {"FOO", "stock_low"})
    void givenAnUnknownUrgency_whenTheAdminGetsTheOverview_thenReturns400NamingTheParameter(String urgency) {
        RequestSpecification request = authenticatedAs("admin@eurder.be", "admin").queryParam("urgency", urgency);

        Response response = request.get("/items");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", containsString("'urgency'"));
    }

    @Test
    void givenACustomer_whenGettingTheOverview_thenReturns403() {
        registerJane();
        RequestSpecification request = authenticatedAs("jane@doe.be", "secret123");

        Response response = request.get("/items");

        response.then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("detail", equalTo("User jane@doe.be has no access to VIEW_ITEM_OVERVIEW"));
    }

    @Test
    void givenNoAuthorizationHeader_whenGettingTheOverview_thenReturns401() {
        RequestSpecification request = RestAssured.given().port(port);

        Response response = request.get("/items");

        response.then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void givenTheApiDocumentation_whenReadingTheOverviewEndpoint_thenTheUrgencyParameterListsTheEnumValues() {
        RequestSpecification request = RestAssured.given().port(port);

        Response response = request.get("/v3/api-docs");

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("paths.'/items'.get.parameters.find { it.name == 'urgency' }.schema.enum",
                        contains("STOCK_LOW", "STOCK_MEDIUM", "STOCK_HIGH"));
    }

    @Test
    void givenItemWithUnitsInTwoStates_whenTheAdminGetsItsUnits_thenListsThemByStateWithTheirAvailability() {
        String id = createItemWithUnits("""
                {"PRISTINE": 2, "DAMAGED": 1}
                """);
        RequestSpecification request = authenticatedAs("admin@eurder.be", "admin");

        Response response = request.get("/items/" + id + "/units");

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("state", contains("PRISTINE", "PRISTINE", "DAMAGED"))
                .body("available", contains(true, true, true))
                .body("[0].keySet()", containsInAnyOrder("id", "state", "available"));
    }

    @Test
    void givenExistingItem_whenTheAdminAddsUnits_thenReturns200WithTheNewAvailableUnits() {
        String id = createItemWithUnits("""
                {"PRISTINE": 2, "DAMAGED": 1}
                """);
        RequestSpecification request = asAdmin("""
                {"state": "PRISTINE", "amount": 10}
                """);

        Response response = request.post("/items/" + id + "/units");

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("availableUnits.PRISTINE", equalTo(12))
                .body("availableUnits.DAMAGED", equalTo(1));
    }

    @Test
    void givenAnAmountOfZero_whenTheAdminAddsUnits_thenReturns400NamingTheAmount() {
        String id = createItem();
        RequestSpecification request = asAdmin("""
                {"state": "PRISTINE", "amount": 0}
                """);

        Response response = request.post("/items/" + id + "/units");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("errors.amount", contains("Amount must be greater than 0"));
    }

    @Test
    void givenAnAvailableUnit_whenTheAdminChangesItsState_thenReturns200AndTheItemCountsItInItsNewState() {
        String id = createItemWithUnits("""
                {"PRISTINE": 2}
                """);
        String unitId = authenticatedAs("admin@eurder.be", "admin").get("/items/" + id + "/units").path("[0].id");
        RequestSpecification request = asAdmin("""
                {"state": "DAMAGED"}
                """);

        Response response = request.patch("/items/" + id + "/units/" + unitId);

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(unitId))
                .body("state", equalTo("DAMAGED"));
        authenticatedAs("admin@eurder.be", "admin").get("/items/" + id + "/units").then()
                .body("state", contains("PRISTINE", "DAMAGED"));
    }

    @Test
    void givenAUnitOfAnotherItem_whenTheAdminChangesItsState_thenReturns404WithBothIds() {
        String id = createItem();
        String unknownUnitId = "3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90";
        RequestSpecification request = asAdmin("""
                {"state": "DAMAGED"}
                """);

        Response response = request.patch("/items/" + id + "/units/" + unknownUnitId);

        response.then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("detail", equalTo("No unit " + unknownUnitId + " found for item " + id));
    }

    @Test
    void givenAnUnknownItem_whenTheAdminGetsItsUnits_thenReturns404() {
        String unknownId = "3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90";
        RequestSpecification request = authenticatedAs("admin@eurder.be", "admin");

        Response response = request.get("/items/" + unknownId + "/units");

        response.then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("detail", equalTo("No item found for id " + unknownId));
    }

    @Test
    void givenACustomer_whenAddingUnits_thenReturns403() {
        String id = createItem();
        registerJane();
        RequestSpecification request = authenticatedAs("jane@doe.be", "secret123", """
                {"state": "PRISTINE", "amount": 1}
                """);

        Response response = request.post("/items/" + id + "/units");

        response.then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void givenNoAuthorizationHeader_whenGettingTheUnitsOfAnItem_thenReturns401() {
        String id = createItem();
        RequestSpecification request = RestAssured.given().port(port);

        Response response = request.get("/items/" + id + "/units");

        response.then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    private String createItemWithUnits(String unitsJson) {
        return asAdmin("""
                {"name": "Chair", "description": "Wooden chair", "price": 49.99, "units": %s}
                """.formatted(unitsJson.strip())).post("/items")
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().path("id");
    }

    private String createItem() {
        return createItem("Chair", 10);
    }

    private String createItem(String name, int stock) {
        return asAdmin(newItem(name, "49.99", stock)).post("/items")
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().path("id");
    }

    private void registerJane() {
        RestAssured.given().port(port).contentType(ContentType.JSON).body("""
                {"firstName": "Jane", "lastName": "Doe", "email": "jane@doe.be", "password": "secret123",
                 "address": {"street": "Rue Haute", "houseNumber": "1", "postalCode": "1000", "city": "Bruxelles"},
                 "phoneNumber": "0470123456"}
                """).post("/customers").then().statusCode(HttpStatus.CREATED.value());
    }

    private RequestSpecification asAdmin(String body) {
        return authenticatedAs("admin@eurder.be", "admin", body);
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

    private static String newItem(String name, String price, int pristineUnits) {
        return """
                {"name": "%s", "description": "Wooden chair", "price": %s, "units": {"PRISTINE": %d}}
                """.formatted(name, price, pristineUnits);
    }

    private static String item(String name, String price, String stock) {
        return """
                {"name": "%s", "description": "Wooden chair", "price": %s, "stock": %s}
                """.formatted(name, price, stock);
    }
}
