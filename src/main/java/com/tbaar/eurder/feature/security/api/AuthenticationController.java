package com.tbaar.eurder.feature.security.api;

import com.tbaar.eurder.feature.security.api.dto.LoginDto;
import com.tbaar.eurder.feature.security.api.dto.RefreshTokenDto;
import com.tbaar.eurder.feature.security.api.dto.TokensDto;
import com.tbaar.eurder.feature.security.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Getting, renewing and revoking tokens")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Log in with an email and a password")
    @ApiResponse(responseCode = "200", description = "Tokens issued", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", description = "Wrong email or password")
    public TokensDto login(@Valid @RequestBody LoginDto request) {
        return authenticationService.login(request);
    }

    @PostMapping(path = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Exchange a refresh token for new tokens",
            description = "A refresh token works once. Using it a second time revokes every token descending from the same login.")
    @ApiResponse(responseCode = "200", description = "New tokens issued", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", description = "Unknown, expired, revoked or already used refresh token")
    public TokensDto refresh(@Valid @RequestBody RefreshTokenDto request) {
        return authenticationService.refresh(request.refreshToken());
    }

    @PostMapping(path = "/logout", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Revoke the refresh tokens of a login",
            description = "The current access token stays valid until it expires.")
    @ApiResponse(responseCode = "204", description = "Logged out, even when the refresh token is unknown")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenDto request) {
        authenticationService.logout(request.refreshToken());
    }
}
