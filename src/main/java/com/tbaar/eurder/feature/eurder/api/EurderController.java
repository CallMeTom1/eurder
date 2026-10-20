package com.tbaar.eurder.feature.eurder.api;

import com.tbaar.eurder.feature.eurder.api.dto.CreateEurderDto;
import com.tbaar.eurder.feature.eurder.api.dto.EurderDto;
import com.tbaar.eurder.feature.eurder.api.dto.ReportDto;
import com.tbaar.eurder.feature.eurder.api.dto.ShippingEurderDto;
import com.tbaar.eurder.feature.eurder.service.EurderService;
import com.tbaar.eurder.feature.security.service.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/eurders")
@Tag(name = "Eurders")
public class EurderController {

    private final EurderService eurderService;

    public EurderController(EurderService eurderService) {
        this.eurderService = eurderService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Order one or more items as the authenticated customer")
    @SecurityRequirement(name = "basicAuth")
    @Secured("ORDER_ITEMS")
    public ResponseEntity<EurderDto> order(@AuthenticationPrincipal AuthenticatedUser customer,
                                           @Valid @RequestBody CreateEurderDto request) {
        EurderDto ordered = eurderService.order(customer.id(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(ordered.id())
                .toUri();
        return ResponseEntity.created(location).body(ordered);
    }

    @GetMapping(path = "/report", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Report of the authenticated customer's eurders with their totals, oldest first")
    @SecurityRequirement(name = "basicAuth")
    @Secured("VIEW_OWN_EURDERS")
    public ReportDto findReport(@AuthenticationPrincipal AuthenticatedUser customer) {
        return eurderService.findReport(customer.id());
    }

    @GetMapping(path = "/shipping-today", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Eurders with item groups to ship today, with the customer's address")
    @SecurityRequirement(name = "basicAuth")
    @Secured("VIEW_SHIPPING_TODAY")
    public List<ShippingEurderDto> findShippingToday() {
        return eurderService.findShippingToday();
    }

    @PostMapping(path = "/{id}/reorder", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Order again the items of one of the authenticated customer's eurders, at the current prices")
    @SecurityRequirement(name = "basicAuth")
    @Secured("ORDER_ITEMS")
    public ResponseEntity<EurderDto> reorder(@AuthenticationPrincipal AuthenticatedUser customer,
                                             @PathVariable UUID id) {
        EurderDto reordered = eurderService.reorder(customer.id(), id);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/eurders/{id}")
                .buildAndExpand(reordered.id())
                .toUri();
        return ResponseEntity.created(location).body(reordered);
    }
}
