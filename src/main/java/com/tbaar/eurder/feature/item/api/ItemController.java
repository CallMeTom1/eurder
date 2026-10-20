package com.tbaar.eurder.feature.item.api;

import com.tbaar.eurder.feature.item.api.dto.AddUnitsDto;
import com.tbaar.eurder.feature.item.api.dto.ChangeUnitStateDto;
import com.tbaar.eurder.feature.item.api.dto.CreateItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemOverviewDto;
import com.tbaar.eurder.feature.item.api.dto.ItemUnitDto;
import com.tbaar.eurder.feature.item.api.dto.UpdateItemDto;
import com.tbaar.eurder.feature.item.domain.StockUrgency;
import com.tbaar.eurder.feature.item.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add an item to the catalog")
    @ApiResponse(responseCode = "201", description = "Item created with its units", useReturnTypeSchema = true)
    @SecurityRequirement(name = "basicAuth")
    @Secured("MANAGE_ITEMS")
    public ResponseEntity<ItemDto> create(@Valid @RequestBody CreateItemDto request) {
        ItemDto created = itemService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Replace the name, description and price of an item")
    @ApiResponse(responseCode = "200", description = "Item updated", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "409", description = "The item was changed by another request meanwhile: retry")
    @SecurityRequirement(name = "basicAuth")
    @Secured("MANAGE_ITEMS")
    public ItemDto update(@PathVariable UUID id, @Valid @RequestBody UpdateItemDto request) {
        return itemService.update(id, request);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List items with their stock urgency, from most to least urgent")
    @SecurityRequirement(name = "basicAuth")
    @Secured("VIEW_ITEM_OVERVIEW")
    public List<ItemOverviewDto> findOverview(@RequestParam(required = false) StockUrgency urgency) {
        return itemService.findOverview(urgency);
    }

    @GetMapping(path = "/{id}/units", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List the units of an item, by state, available ones first")
    @SecurityRequirement(name = "basicAuth")
    @Secured("MANAGE_ITEMS")
    public List<ItemUnitDto> findUnits(@PathVariable UUID id) {
        return itemService.findUnits(id);
    }

    @PostMapping(path = "/{id}/units", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add units of a state to the stock of an item")
    @SecurityRequirement(name = "basicAuth")
    @Secured("MANAGE_ITEMS")
    public ItemDto addUnits(@PathVariable UUID id, @Valid @RequestBody AddUnitsDto request) {
        return itemService.addUnits(id, request);
    }

    @PatchMapping(path = "/{id}/units/{unitId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Change the state of an available unit of an item")
    @ApiResponse(responseCode = "200", description = "Unit in its new state", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "409", description = "The unit is already sold, or the item was changed by another request meanwhile")
    @SecurityRequirement(name = "basicAuth")
    @Secured("MANAGE_ITEMS")
    public ItemUnitDto changeUnitState(@PathVariable UUID id, @PathVariable UUID unitId, @Valid @RequestBody ChangeUnitStateDto request) {
        return itemService.changeUnitState(id, unitId, request);
    }
}
