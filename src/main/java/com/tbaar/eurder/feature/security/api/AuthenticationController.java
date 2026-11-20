package com.tbaar.eurder.feature.security.api;

import com.tbaar.eurder.feature.security.api.dto.CurrentUserDto;
import com.tbaar.eurder.feature.security.api.dto.LoginDto;
import com.tbaar.eurder.feature.security.authentication.AuthenticatedUser;
import com.tbaar.eurder.feature.security.service.AuthenticationResult;
import com.tbaar.eurder.feature.security.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Getting, renewing and revoking the token cookies, and the logged-in user")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final AuthCookies authCookies;

    public AuthenticationController(AuthenticationService authenticationService, AuthCookies authCookies) {
        this.authenticationService = authenticationService;
        this.authCookies = authCookies;
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Log in with an email and a password",
            description = "Sets the access_token and refresh_token cookies, both HttpOnly. The body describes the user, without any token.")
    @ApiResponse(responseCode = "200", description = "Logged in, token cookies set", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", description = "Wrong email or password")
    public ResponseEntity<CurrentUserDto> login(@Valid @RequestBody LoginDto request) {
        return withTokenCookies(authenticationService.login(request));
    }

    @PostMapping(path = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Exchange the refresh_token cookie for new token cookies",
            description = "A refresh token works once. Using it a second time revokes every token descending from the same login.")
    @ApiResponse(responseCode = "200", description = "New token cookies set", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", description = "Missing, unknown, expired, revoked or already used refresh token")
    public ResponseEntity<CurrentUserDto> refresh(
            @Parameter(hidden = true) @CookieValue(name = AuthCookies.REFRESH_TOKEN, required = false) String refreshToken) {
        return withTokenCookies(authenticationService.refresh(refreshToken));
    }

    @GetMapping(path = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Describe the logged-in user",
            description = "A front end calls it at startup: it cannot read the HttpOnly cookies, so it asks who they belong to.")
    @ApiResponse(responseCode = "200", description = "The logged-in user", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token")
    @SecurityRequirement(name = "bearerAuth")
    public CurrentUserDto me(@AuthenticationPrincipal AuthenticatedUser user) {
        return authenticationService.findCurrentUser(user);
    }

    @PostMapping(path = "/logout")
    @Operation(summary = "Revoke the refresh tokens of a login and clear the token cookies",
            description = "An access token already copied elsewhere stays valid until it expires.")
    @ApiResponse(responseCode = "204", description = "Logged out, even without a cookie or with an unknown refresh token")
    public ResponseEntity<Void> logout(
            @Parameter(hidden = true) @CookieValue(name = AuthCookies.REFRESH_TOKEN, required = false) String refreshToken) {
        authenticationService.logout(refreshToken);
        return ResponseEntity.noContent().headers(authCookies.clear()).build();
    }

    private ResponseEntity<CurrentUserDto> withTokenCookies(AuthenticationResult result) {
        return ResponseEntity.ok()
                .headers(authCookies.issue(result.accessToken(), result.refreshToken()))
                .body(result.user());
    }
}
