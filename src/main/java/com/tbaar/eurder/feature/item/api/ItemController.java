package com.tbaar.eurder.feature.item.api;

import com.tbaar.eurder.feature.item.api.dto.AddUnitsDto;
import com.tbaar.eurder.feature.item.api.dto.ChangeUnitStateDto;
import com.tbaar.eurder.feature.item.api.dto.CreateItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemOverviewDto;
import com.tbaar.eurder.feature.item.api.dto.UpdateItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemUnitDto;
import com.tbaar.eurder.feature.item.domain.StockUrgency;
import com.tbaar.eurder.feature.item.service.ItemService;
import com.tbaar.eurder.feature.security.domain.Feature;
import com.tbaar.eurder.feature.security.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/items")
@Tag(name = "Items")
public class ItemController {

    private final ItemService itemService;
    private final SecurityService securityService;

    public ItemController(ItemService itemService, SecurityService securityService) {
        this.itemService = itemService;
        this.securityService = securityService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add an item to the catalog")
    @SecurityRequirement(name = "basicAuth")
    public ResponseEntity<ItemDto> create(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                          @Valid @RequestBody CreateItemDto request) {
        securityService.validateAuthorization(authorization, Feature.MANAGE_ITEMS);
        ItemDto created = itemService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Replace the name, description, price and stock of an item")
    @SecurityRequirement(name = "basicAuth")
    public ItemDto update(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                          @PathVariable UUID id,
                          @Valid @RequestBody UpdateItemDto request) {
        securityService.validateAuthorization(authorization, Feature.MANAGE_ITEMS);
        return itemService.update(id, request);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List items with their stock urgency, from most to least urgent")
    @SecurityRequirement(name = "basicAuth")
    public List<ItemOverviewDto> findOverview(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                              @RequestParam(required = false) StockUrgency urgency) {
        securityService.validateAuthorization(authorization, Feature.VIEW_ITEM_OVERVIEW);
        return itemService.findOverview(urgency);
    }

    @GetMapping(path = "/{id}/units", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List the units of an item, by state, available ones first")
    @SecurityRequirement(name = "basicAuth")
    public List<ItemUnitDto> findUnits(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                       @PathVariable UUID id) {
        securityService.validateAuthorization(authorization, Feature.MANAGE_ITEMS);
        return itemService.findUnits(id);
    }

    @PostMapping(path = "/{id}/units", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add units of a state to the stock of an item")
    @SecurityRequirement(name = "basicAuth")
    public ItemDto addUnits(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                            @PathVariable UUID id,
                            @Valid @RequestBody AddUnitsDto request) {
        securityService.validateAuthorization(authorization, Feature.MANAGE_ITEMS);
        return itemService.addUnits(id, request);
    }

    @PatchMapping(path = "/{id}/units/{unitId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Change the state of an available unit of an item")
    @SecurityRequirement(name = "basicAuth")
    public ItemUnitDto changeUnitState(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                       @PathVariable UUID id,
                                       @PathVariable UUID unitId,
                                       @Valid @RequestBody ChangeUnitStateDto request) {
        securityService.validateAuthorization(authorization, Feature.MANAGE_ITEMS);
        return itemService.changeUnitState(id, unitId, request);
    }
}
