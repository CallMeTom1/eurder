package com.tbaar.eurder.common;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiEndToEndTest {

    @LocalServerPort
    private int port;

    private JsonPath apiDocs;

    @BeforeEach
    void readTheApiDocumentation() {
        apiDocs = RestAssured.given().port(port).get("/v3/api-docs").jsonPath();
    }

    @Test
    void givenTheApiDocumentation_whenReadingTheSchemas_thenDescribesTheProblemDetailErrorFormat() {
        String problemDetail = "components.schemas.ProblemDetail.properties";

        Map<String, Object> properties = apiDocs.getMap(problemDetail);

        assertThat(properties).containsKeys("type", "title", "status", "detail", "instance", "errors");
    }

    @Test
    void givenASecuredEndpointWithABody_whenReadingItsResponses_thenListsTheValidationAndSecurityErrors() {
        String responses = "paths.'/items'.post.responses";

        Map<String, Object> codes = apiDocs.getMap(responses);

        assertThat(codes).containsKeys("400", "401", "403");
        assertThat(apiDocs.getString(responses + ".'401'.content.'application/problem+json'.schema.'$ref'"))
                .isEqualTo("#/components/schemas/ProblemDetail");
    }

    @Test
    void givenThePublicRegistrationEndpoint_whenReadingItsResponses_thenListsNoSecurityError() {
        String responses = "paths.'/customers'.post.responses";

        Map<String, Object> codes = apiDocs.getMap(responses);

        assertThat(codes).containsKey("400").doesNotContainKeys("401", "403");
    }

    @Test
    void givenAnEndpointWithAnIdInItsPath_whenReadingItsResponses_thenListsNotFound() {
        String responses = "paths.'/customers/{id}'.get.responses";

        Map<String, Object> codes = apiDocs.getMap(responses);

        assertThat(codes).containsKeys("200", "400", "401", "403", "404");
    }

    @Test
    void givenAnEndpointWithoutParameterNorBody_whenReadingItsResponses_thenListsNoValidationError() {
        String responses = "paths.'/eurders/report'.get.responses";

        Map<String, Object> codes = apiDocs.getMap(responses);

        assertThat(codes).containsKeys("200", "401", "403").doesNotContainKeys("400", "404");
    }

    @Test
    void givenACreationEndpoint_whenReadingItsResponses_thenDocuments201WithTheCreatedResourceInsteadOf200() {
        String responses = "paths.'/items'.post.responses";

        Map<String, Object> codes = apiDocs.getMap(responses);

        assertThat(codes).containsKey("201").doesNotContainKey("200");
        assertThat(apiDocs.getString(responses + ".'201'.content.'application/json'.schema.'$ref'"))
                .isEqualTo("#/components/schemas/ItemDto");
    }

    @Test
    void givenTheRegistrationEndpoint_whenReadingItsResponses_thenDocumentsTheConflictOfAnEmailAlreadyInUse() {
        String responses = "paths.'/customers'.post.responses";

        Map<String, Object> codes = apiDocs.getMap(responses);

        assertThat(codes).containsKeys("201", "409").doesNotContainKey("200");
        assertThat(apiDocs.getString(responses + ".'409'.content.'application/problem+json'.schema.'$ref'"))
                .isEqualTo("#/components/schemas/ProblemDetail");
    }

    @Test
    void givenTheOrderEndpoints_whenReadingTheirResponses_thenDocumentTheUnknownItemAndTheConcurrentChange() {
        Map<String, Object> order = apiDocs.getMap("paths.'/eurders'.post.responses");
        Map<String, Object> reorder = apiDocs.getMap("paths.'/eurders/{id}/reorder'.post.responses");

        assertThat(order).containsKeys("201", "404", "409").doesNotContainKey("200");
        assertThat(reorder).containsKeys("201", "404", "409").doesNotContainKey("200");
    }

    @Test
    void givenEndpointsThatCanConflict_whenReadingTheirResponses_thenKeep200AndAdd409() {
        Map<String, Object> update = apiDocs.getMap("paths.'/items/{id}'.put.responses");
        Map<String, Object> changeState = apiDocs.getMap("paths.'/items/{id}/units/{unitId}'.patch.responses");

        assertThat(update).containsKeys("200", "409");
        assertThat(changeState).containsKeys("200", "409");
    }

    @Test
    void givenASecuredEndpoint_whenReadingItsDescription_thenStatesTheRequiredFeatureAndTheRolesHavingIt() {
        String description = apiDocs.getString("paths.'/items'.post.description");

        assertThat(description).contains("Requires the MANAGE_ITEMS feature (role ADMIN)");
    }

    @Test
    void givenThePublicRegistrationEndpoint_whenReadingItsDescription_thenStatesThatNoAuthenticationIsNeeded() {
        String description = apiDocs.getString("paths.'/customers'.post.description");

        assertThat(description).contains("Public: no authentication needed");
    }

    @Test
    void givenTheApiDocumentation_whenReadingTheTags_thenEachTagHasADescription() {
        List<Map<String, String>> tags = apiDocs.getList("tags");

        assertThat(tags)
                .extracting(tag -> tag.get("name"))
                .containsExactlyInAnyOrder("Customers", "Items", "Eurders");
        assertThat(tags).allSatisfy(tag -> assertThat(tag.get("description")).isNotBlank());
    }

    @Test
    void givenTheApiDocumentation_whenReadingItsDescription_thenExplainsHowToAuthenticateAndTheErrorFormat() {
        String description = apiDocs.getString("info.description");

        assertThat(description).contains("HTTP Basic").contains("ProblemDetail");
    }

    @Test
    void givenMapsKeyedByAState_whenReadingTheirSchemas_thenTheKeysAreLimitedToTheStates() {
        List<String> unitsKeys = apiDocs.getList("components.schemas.CreateItemDto.properties.units.propertyNames.enum");
        List<String> availableUnitsKeys = apiDocs.getList("components.schemas.ItemDto.properties.availableUnits.propertyNames.enum");

        assertThat(unitsKeys).containsExactly("PRISTINE", "SLIGHTLY_DAMAGED", "DAMAGED");
        assertThat(availableUnitsKeys).containsExactly("PRISTINE", "SLIGHTLY_DAMAGED", "DAMAGED");
    }

    @Test
    void givenTheRequestBodies_whenReadingTheirFields_thenEachFieldHasADescriptionAndAnExample() {
        String price = "components.schemas.CreateItemDto.properties.price";
        String state = "components.schemas.ItemGroupRequestDto.properties.state";
        String email = "components.schemas.CreateCustomerDto.properties.email";

        assertThat(apiDocs.getString(price + ".description")).contains("2 decimals");
        assertThat(apiDocs.getDouble(price + ".example")).isEqualTo(49.99);
        assertThat(apiDocs.getString(state + ".description")).contains("PRISTINE when left out");
        assertThat(apiDocs.getString(email + ".example")).isEqualTo("jane@doe.be");
    }
}
