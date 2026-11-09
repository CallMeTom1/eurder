package com.tbaar.eurder.feature.security.api;

import com.tbaar.eurder.feature.security.api.dto.LoginDto;
import com.tbaar.eurder.feature.security.api.dto.TokensDto;
import com.tbaar.eurder.feature.security.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Getting tokens")
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
}
