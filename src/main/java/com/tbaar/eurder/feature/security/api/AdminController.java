package com.tbaar.eurder.feature.security.api;

import com.tbaar.eurder.feature.security.api.dto.AdminDto;
import com.tbaar.eurder.feature.security.api.dto.CreateAdminDto;
import com.tbaar.eurder.feature.security.service.CredentialsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/administrators")
@Tag(name = "Administrators", description = "Administrator accounts, created by another admin")
public class AdminController {

    private final CredentialsService credentialsService;

    public AdminController(CredentialsService credentialsService) {
        this.credentialsService = credentialsService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Register a new admin account")
    @ApiResponse(responseCode = "201", description = "Admin account created", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "409", description = "Email already in use")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "basicAuth")
    @Secured("REGISTER_ADMIN")
    public AdminDto registerAsAnAdmin(@Valid @RequestBody CreateAdminDto request) {
        return credentialsService.createAdmin(request);
    }
}
