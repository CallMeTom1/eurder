package com.tbaar.eurder.feature.security;

import com.tbaar.eurder.common.AccessTokens;
import com.tbaar.eurder.common.EurderApi;
import com.tbaar.eurder.common.MutableClock;
import com.tbaar.eurder.common.UniqueEmail;
import com.tbaar.eurder.feature.security.domain.RefreshTokenRepository;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import static com.tbaar.eurder.common.ValidationErrors.codesOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthenticationEndToEndTest {

    private static final ZonedDateTime NOON = ZonedDateTime.of(2026, 6, 15, 12, 0, 0, 0, ZoneId.of("Europe/Brussels"));
    private static final String ADMIN = "admin@eurder.be";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final long SEVEN_DAYS_IN_SECONDS = 7 * 24 * 60 * 60;

    @TestConfiguration
    static class MutableClockConfig {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(NOON);
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private MutableClock clock;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private final String email = UniqueEmail.of("jane");

    @BeforeEach
    void resetClock() {
        clock.setTo(NOON);
    }

    @ParameterizedTest(name = "Authorization: \"{0}\" is rejected with {1}")
    @CsvSource(delimiter = '|', value = {
            "''                                       | error.security.authentication-required",
            "Bearer some-token                        | error.security.invalid-access-token",
            "Bearer !!!                               | error.security.invalid-access-token",
            "Basic YWRtaW5AZXVyZGVyLmJlOmFkbWlu       | error.security.authentication-required"})
    void givenAMissingOrInvalidHeader_whenCallingAProtectedEndpoint_thenReturns401WithABearerChallenge(String authorization, String code) {
        RequestSpecification request = EurderApi.given(port).header(HttpHeaders.AUTHORIZATION, authorization);

        Response response = request.get("/customers");

        response.then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"Eurder\"")
                .body("detail", equalTo("Invalid credentials"))
                .body("code", equalTo(code));
    }

    @Test
    void givenTheRightPassword_whenLoggingIn_thenSetsTheTokensInUncacheableCookiesThatOpenTheApi() {
        Response response = login(ADMIN, ADMIN_PASSWORD);

        response.then()
                .statusCode(HttpStatus.OK.value())
                .header(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("no-store"));
        assertThat(response.getDetailedCookie(ACCESS_TOKEN)).satisfies(cookie -> {
            assertThat(cookie.getPath()).isEqualTo("/api");
            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.isSecured()).isTrue();
            assertThat(cookie.getSameSite()).isEqualTo("Strict");
            assertThat(cookie.getMaxAge()).isEqualTo(SEVEN_DAYS_IN_SECONDS);
        });
        assertThat(response.getDetailedCookie(REFRESH_TOKEN)).satisfies(cookie -> {
            assertThat(cookie.getPath()).isEqualTo("/api/auth");
            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.isSecured()).isTrue();
            assertThat(cookie.getSameSite()).isEqualTo("Strict");
            assertThat(cookie.getMaxAge()).isEqualTo(SEVEN_DAYS_IN_SECONDS);
        });
        withAccessToken(response.getCookie(ACCESS_TOKEN)).get("/customers").then().statusCode(HttpStatus.OK.value());
    }

    @Test
    void givenTheAdmin_whenLoggingIn_thenTheBodyDescribesTheUserWithoutAnyToken() {
        Response response = login(ADMIN, ADMIN_PASSWORD);

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("id", notNullValue())
                .body("role", equalTo("ADMIN"))
                .body("email", equalTo(ADMIN))
                .body("firstName", nullValue())
                .body("lastName", nullValue())
                .body("$", not(hasKey("accessToken")))
                .body("$", not(hasKey("refreshToken")));
    }

    @Test
    void givenACustomer_whenLoggingIn_thenTheBodyCarriesTheirName() {
        registerCustomer(email, "secret123");

        Response response = login(email, "secret123");

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("role", equalTo("CUSTOMER"))
                .body("email", equalTo(email))
                .body("firstName", equalTo("Jane"))
                .body("lastName", equalTo("Doe"));
    }

    @Test
    void givenAWrongPasswordOrAnUnknownEmail_whenLoggingIn_thenBothGetTheSame401() {
        Response wrongPassword = login(ADMIN, "not-admin");
        Response unknownEmail = login("nobody@eurder.be", ADMIN_PASSWORD);

        wrongPassword.then().statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("detail", equalTo("Invalid credentials"))
                .body("code", equalTo("error.security.invalid-credentials"));
        unknownEmail.then().statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("detail", equalTo("Invalid credentials"))
                .body("code", equalTo("error.security.invalid-credentials"));
    }

    @Test
    void givenAnEmailWithCapitalsAndSpaces_whenLoggingIn_thenAuthenticatesTheAdmin() {
        Response response = login(" Admin@Eurder.BE ", ADMIN_PASSWORD);

        response.then().statusCode(HttpStatus.OK.value());
    }

    @Test
    void givenABlankPassword_whenLoggingIn_thenReturns400NamingTheField() {
        Response response = login(ADMIN, " ");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(codesOf("password"), contains("validation.required"));
    }

    @Test
    void givenACustomer_whenLoggingIn_thenTheirTokenOpensTheirOwnEndpoints() {
        registerCustomer(email, "secret123");

        String accessToken = AccessTokens.of(port, email, "secret123");

        withAccessToken(accessToken).get("/eurders/report").then().statusCode(HttpStatus.OK.value());
    }

    @Test
    void givenTheAdmin_whenCallingACustomerEndpoint_thenReturns403NamingTheMissingFeature() {
        RequestSpecification request = withAccessToken(AccessTokens.of(port, ADMIN, ADMIN_PASSWORD));

        Response response = request.get("/eurders/report");

        response.then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("detail", equalTo("You have no access to VIEW_OWN_EURDERS"))
                .body("code", equalTo("error.security.forbidden"))
                .body("params.features", contains("VIEW_OWN_EURDERS"));
    }

    @Test
    void givenAnAccessToken_whenReadingItsPayload_thenCarriesTheRoleButNoPersonalData() {
        String accessToken = AccessTokens.of(port, ADMIN, ADMIN_PASSWORD);

        String payload = new String(Base64.getUrlDecoder().decode(accessToken.split("\\.")[1]), StandardCharsets.UTF_8);

        assertThat(payload).contains("\"role\":\"ADMIN\"", "\"iss\":\"eurder\"", "\"sub\":\"").doesNotContain(ADMIN, "password");
    }

    @Test
    void givenAnAccessToken_whenItsLifetimeEnds_thenItIsNoLongerAcceptedAndTheFrontEndIsToldToRefreshIt() {
        String accessToken = AccessTokens.of(port, ADMIN, ADMIN_PASSWORD);

        clock.setTo(NOON.plusMinutes(15));
        withAccessToken(accessToken).get("/customers").then().statusCode(HttpStatus.OK.value());

        clock.setTo(NOON.plusMinutes(15).plusSeconds(1));
        withAccessToken(accessToken).get("/customers").then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("code", equalTo("error.security.access-token-expired"));
    }

    @Test
    void givenAnAccessTokenSignedWithAnotherSecret_whenCallingTheApi_thenReturns401() {
        JwtEncoder otherEncoder = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(
                "another-secret-that-is-long-enough-for-hs256".getBytes(StandardCharsets.UTF_8), "HmacSHA256")).build();

        String forged = sign(otherEncoder, "eurder", UUID.randomUUID().toString(), "ADMIN");

        withAccessToken(forged).get("/customers").then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void givenAnUnsignedAccessToken_whenCallingTheApi_thenReturns401() {
        String header = base64("{\"alg\":\"none\"}");
        String payload = base64("{\"iss\":\"eurder\",\"sub\":\"%s\",\"role\":\"ADMIN\",\"exp\":%d}"
                .formatted(UUID.randomUUID(), NOON.plusHours(1).toEpochSecond()));

        withAccessToken(header + "." + payload + ".").get("/customers").then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void givenAnAccessTokenOfAnotherIssuer_whenCallingTheApi_thenReturns401() {
        String token = sign(jwtEncoder, "someone-else", UUID.randomUUID().toString(), "ADMIN");

        withAccessToken(token).get("/customers").then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void givenASignedAccessTokenWithAnUnknownRole_whenCallingTheApi_thenReturns401InsteadOfCrashing() {
        String token = sign(jwtEncoder, "eurder", UUID.randomUUID().toString(), "NOT_A_ROLE");

        withAccessToken(token).get("/customers").then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void givenASignedAccessTokenWithAnInvalidSubject_whenCallingTheApi_thenReturns401InsteadOfCrashing() {
        String token = sign(jwtEncoder, "eurder", "not-a-uuid", "ADMIN");

        withAccessToken(token).get("/customers").then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void givenARefreshTokenCookie_whenRefreshing_thenSetsNewTokenCookiesThatWork() {
        String refreshToken = login(ADMIN, ADMIN_PASSWORD).getCookie(REFRESH_TOKEN);
        clock.setTo(NOON.plusHours(1));

        Response response = refresh(refreshToken);

        response.then().statusCode(HttpStatus.OK.value()).body("email", equalTo(ADMIN));
        assertThat(response.getCookie(REFRESH_TOKEN)).isNotEqualTo(refreshToken);
        assertThat(response.getDetailedCookie(REFRESH_TOKEN).getPath()).isEqualTo("/api/auth");
        withAccessToken(response.getCookie(ACCESS_TOKEN)).get("/customers").then().statusCode(HttpStatus.OK.value());
    }

    @Test
    void givenAnExpiredAccessTokenSentAlong_whenRefreshing_thenTheRefreshStillWorks() {
        Response tokens = login(ADMIN, ADMIN_PASSWORD);
        clock.setTo(NOON.plusHours(1));

        Response response = EurderApi.given(port)
                .cookie(REFRESH_TOKEN, tokens.getCookie(REFRESH_TOKEN))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.getCookie(ACCESS_TOKEN))
                .post("/auth/refresh");

        response.then().statusCode(HttpStatus.OK.value());
    }

    @Test
    void givenAnAlreadyUsedRefreshToken_whenReusingIt_thenReturns401AndRevokesTheWholeLogin() {
        String stolen = login(ADMIN, ADMIN_PASSWORD).getCookie(REFRESH_TOKEN);
        String latest = refresh(stolen).getCookie(REFRESH_TOKEN);

        Response reuse = refresh(stolen);

        reuse.then().statusCode(HttpStatus.UNAUTHORIZED.value());
        refresh(latest).then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void givenTwoLogins_whenOneIsCompromised_thenTheOtherKeepsWorking() {
        String compromised = login(ADMIN, ADMIN_PASSWORD).getCookie(REFRESH_TOKEN);
        String otherDevice = login(ADMIN, ADMIN_PASSWORD).getCookie(REFRESH_TOKEN);
        refresh(compromised).then().statusCode(HttpStatus.OK.value());

        refresh(compromised).then().statusCode(HttpStatus.UNAUTHORIZED.value());

        refresh(otherDevice).then().statusCode(HttpStatus.OK.value());
    }

    @Test
    void givenTheSameRefreshTokenSentTwiceAtOnce_whenRefreshing_thenOnlyOneSucceedsAndTheLoginIsRevoked() {
        String refreshToken = login(ADMIN, ADMIN_PASSWORD).getCookie(REFRESH_TOKEN);
        CountDownLatch start = new CountDownLatch(1);

        List<CompletableFuture<Response>> requests = List.of(refreshOnceReleased(refreshToken, start), refreshOnceReleased(refreshToken, start));
        start.countDown();
        List<Response> responses = requests.stream().map(CompletableFuture::join).toList();

        assertThat(responses).extracting(Response::statusCode)
                .containsExactlyInAnyOrder(HttpStatus.OK.value(), HttpStatus.UNAUTHORIZED.value());
        String winner = responses.stream().filter(response -> response.statusCode() == HttpStatus.OK.value()).findFirst().orElseThrow().getCookie(REFRESH_TOKEN);
        refresh(winner).then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void givenAnUnknownRefreshToken_whenRefreshing_thenReturns401() {
        refresh("unknown-token").then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("detail", equalTo("Invalid credentials"))
                .body("code", equalTo("error.security.invalid-refresh-token"));
    }

    @Test
    void givenNoRefreshTokenCookie_whenRefreshing_thenReturns401() {
        RequestSpecification request = EurderApi.given(port);

        Response response = request.post("/auth/refresh");

        response.then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("code", equalTo("error.security.invalid-refresh-token"));
    }

    @Test
    void givenAnExpiredRefreshToken_whenRefreshing_thenReturns401() {
        String refreshToken = login(ADMIN, ADMIN_PASSWORD).getCookie(REFRESH_TOKEN);

        clock.setTo(NOON.plusDays(7));

        refresh(refreshToken).then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void givenARefreshToken_whenLookingInTheDatabase_thenOnlyItsHashIsStored() throws Exception {
        String refreshToken = login(ADMIN, ADMIN_PASSWORD).getCookie(REFRESH_TOKEN);

        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(refreshToken.getBytes(StandardCharsets.UTF_8)));

        assertThat(refreshTokenRepository.findByTokenHash(refreshToken)).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash(hash)).isPresent();
    }

    @Test
    void givenALogin_whenLoggingOut_thenTheRefreshTokenIsRefusedButTheAccessTokenLivesUntilItExpires() {
        Response tokens = login(ADMIN, ADMIN_PASSWORD);

        Response logout = logout(tokens.getCookie(REFRESH_TOKEN));

        logout.then().statusCode(HttpStatus.NO_CONTENT.value());
        refresh(tokens.getCookie(REFRESH_TOKEN)).then().statusCode(HttpStatus.UNAUTHORIZED.value());
        withAccessToken(tokens.getCookie(ACCESS_TOKEN)).get("/customers").then().statusCode(HttpStatus.OK.value());
    }

    @Test
    void givenALogin_whenLoggingOut_thenBothCookiesAreCleared() {
        String refreshToken = login(ADMIN, ADMIN_PASSWORD).getCookie(REFRESH_TOKEN);

        Response logout = logout(refreshToken);

        assertThat(logout.getDetailedCookie(ACCESS_TOKEN)).satisfies(cookie -> {
            assertThat(cookie.getValue()).isEmpty();
            assertThat(cookie.getPath()).isEqualTo("/api");
            assertThat(cookie.getMaxAge()).isZero();
        });
        assertThat(logout.getDetailedCookie(REFRESH_TOKEN)).satisfies(cookie -> {
            assertThat(cookie.getValue()).isEmpty();
            assertThat(cookie.getPath()).isEqualTo("/api/auth");
            assertThat(cookie.getMaxAge()).isZero();
        });
    }

    @Test
    void givenAnUnknownRefreshToken_whenLoggingOut_thenReturns204WithoutRevealingAnything() {
        logout("unknown-token").then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void givenNoRefreshTokenCookie_whenLoggingOut_thenReturns204() {
        RequestSpecification request = EurderApi.given(port);

        Response response = request.post("/auth/logout");

        response.then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    private CompletableFuture<Response> refreshOnceReleased(String refreshToken, CountDownLatch start) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                start.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            return refresh(refreshToken);
        });
    }

    private void registerCustomer(String customerEmail, String password) {
        jsonWithBody("""
                {"firstName": "Jane", "lastName": "Doe", "email": "%s", "password": "%s",
                 "address": {"street": "Rue Haute", "houseNumber": "1", "postalCode": "1000", "city": "Bruxelles"},
                 "phoneNumber": "0470123456"}
                """.formatted(customerEmail, password)).post("/customers").then().statusCode(HttpStatus.CREATED.value());
    }

    private Response login(String loginEmail, String password) {
        return jsonWithBody("""
                {"email": "%s", "password": "%s"}
                """.formatted(loginEmail, password)).post("/auth/login");
    }

    private Response refresh(String refreshToken) {
        return EurderApi.given(port).cookie(REFRESH_TOKEN, refreshToken).post("/auth/refresh");
    }

    private Response logout(String refreshToken) {
        return EurderApi.given(port).cookie(REFRESH_TOKEN, refreshToken).post("/auth/logout");
    }

    private RequestSpecification jsonWithBody(String body) {
        return EurderApi.given(port).contentType(ContentType.JSON).body(body);
    }

    private RequestSpecification withAccessToken(String accessToken) {
        return EurderApi.given(port).auth().oauth2(accessToken);
    }

    private String sign(JwtEncoder encoder, String issuer, String subject, String role) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .claim("role", role)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private static String base64(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
