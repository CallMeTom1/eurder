package com.tbaar.eurder.feature.eurder.api;

import com.tbaar.eurder.feature.eurder.api.dto.CreateEurderDto;
import com.tbaar.eurder.feature.eurder.api.dto.EurderDto;
import com.tbaar.eurder.feature.eurder.service.EurderService;
import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.Feature;
import com.tbaar.eurder.feature.security.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/eurders")
@Tag(name = "Eurders")
public class EurderController {

    private final EurderService eurderService;
    private final SecurityService securityService;

    public EurderController(EurderService eurderService, SecurityService securityService) {
        this.eurderService = eurderService;
        this.securityService = securityService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Order one or more items as the authenticated customer")
    @SecurityRequirement(name = "basicAuth")
    public ResponseEntity<EurderDto> order(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                           @Valid @RequestBody CreateEurderDto request) {
        Credentials customer = securityService.validateAuthorization(authorization, Feature.ORDER_ITEMS);
        EurderDto ordered = eurderService.order(customer.getId(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(ordered.id())
                .toUri();
        return ResponseEntity.created(location).body(ordered);
    }
}
