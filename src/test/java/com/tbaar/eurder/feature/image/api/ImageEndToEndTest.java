package com.tbaar.eurder.feature.image.api;

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

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ImageEndToEndTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 0, 0, 0, 13};
    private static final byte[] JPEG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0, 0, 16, 'J', 'F', 'I', 'F'};

    @LocalServerPort
    private int port;

    @Test
    void givenAPngImage_whenTheAdminUploadsIt_thenReturns201WithItsPathAndAnybodyCanReadIt() {
        Response response = asAdmin().multiPart("file", "chair.png", PNG, "image/png").post("/images");

        response.then()
                .statusCode(HttpStatus.CREATED.value())
                .body("path", matchesPattern("images/[0-9a-f-]{36}\\.png"));
        byte[] read = EurderApi.given(port).get("/" + response.path("path")).then()
                .statusCode(HttpStatus.OK.value())
                .contentType("image/png")
                .extract().asByteArray();
        assertThat(read).isEqualTo(PNG);
    }

    @Test
    void givenAJpegImage_whenTheAdminUploadsIt_thenItIsStoredAsAJpeg() {
        Response response = asAdmin().multiPart("file", "lamp.jpeg", JPEG, "image/jpeg").post("/images");

        response.then()
                .statusCode(HttpStatus.CREATED.value())
                .body("path", matchesPattern("images/[0-9a-f-]{36}\\.jpg"));
    }

    @Test
    void givenATextFileNamedLikeAnImage_whenTheAdminUploadsIt_thenReturns400WithTheImageCode() {
        Response response = asAdmin().multiPart("file", "chair.png", "not an image".getBytes(), "image/png").post("/images");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("error.image.invalid-type"));
    }

    @Test
    void givenAFileOfMoreThan2Megabytes_whenTheAdminUploadsIt_thenReturns413WithTheSizeCode() {
        byte[] big = Arrays.copyOf(PNG, 3 * 1024 * 1024);

        Response response = asAdmin().multiPart("file", "big.png", big, "image/png").post("/images");

        response.then()
                .statusCode(HttpStatus.PAYLOAD_TOO_LARGE.value())
                .body("code", equalTo("error.common.file-too-large"));
    }

    @Test
    void givenACustomer_whenUploadingAnImage_thenReturns403() {
        String email = UniqueEmail.of("jane");
        EurderApi.given(port).contentType(ContentType.JSON).body("""
                {"firstName": "Jane", "lastName": "Doe", "email": "%s", "password": "secret123",
                 "address": {"street": "Rue Haute", "houseNumber": "1", "postalCode": "1000", "city": "Bruxelles", "country": "BELGIUM"},
                 "phoneNumber": "0470123456"}
                """.formatted(email)).post("/customers");
        RequestSpecification request = EurderApi.given(port).auth().oauth2(AccessTokens.of(port, email, "secret123"));

        Response response = request.multiPart("file", "chair.png", PNG, "image/png").post("/images");

        response.then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void givenNoAuthorizationHeader_whenUploadingAnImage_thenReturns401() {
        RequestSpecification request = EurderApi.given(port);

        Response response = request.multiPart("file", "chair.png", PNG, "image/png").post("/images");

        response.then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void givenAnUnknownImage_whenReadingIt_thenReturns404WithTheImageCode() {
        RequestSpecification request = EurderApi.given(port);

        Response response = request.get("/images/3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90.png");

        response.then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo("error.image.not-found"));
    }

    @Test
    void givenANameThatIsNotOneOfOurImages_whenReadingIt_thenReturns404WithoutLookingOutsideTheFolder() {
        RequestSpecification request = EurderApi.given(port).urlEncodingEnabled(false);

        Response response = request.get("/images/..%2Fapplication.properties");

        assertThat(response.statusCode()).isIn(HttpStatus.NOT_FOUND.value(), HttpStatus.BAD_REQUEST.value());
    }

    private RequestSpecification asAdmin() {
        return EurderApi.given(port).auth().oauth2(AccessTokens.of(port, "admin@eurder.be", "admin"));
    }
}
