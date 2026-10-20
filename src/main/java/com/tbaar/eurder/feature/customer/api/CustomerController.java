package com.tbaar.eurder.feature.customer.api;

import com.tbaar.eurder.feature.customer.api.dto.CreateCustomerDto;
import com.tbaar.eurder.feature.customer.api.dto.CustomerDto;
import com.tbaar.eurder.feature.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
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
@RequestMapping("/customers")
@Tag(name = "Customers", description = "Customer accounts: registration, and their details for the admin")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Register a new customer account")
    @ApiResponse(responseCode = "201", description = "Customer account created", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "409", description = "Email already in use")
    public ResponseEntity<CustomerDto> register(@Valid @RequestBody CreateCustomerDto request) {
        CustomerDto registered = customerService.register(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(registered.id())
                .toUri();
        return ResponseEntity.created(location).body(registered);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List all customers, sorted by last name then first name")
    @SecurityRequirement(name = "basicAuth")
    @Secured("VIEW_CUSTOMERS")
    public List<CustomerDto> findAll() {
        return customerService.findAll();
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the details of one customer")
    @SecurityRequirement(name = "basicAuth")
    @Secured("VIEW_CUSTOMERS")
    public CustomerDto findById(@PathVariable UUID id) {
        return customerService.findById(id);
    }
}
