package com.tbaar.eurder.feature.item.api;

import com.tbaar.eurder.feature.item.api.dto.CatalogItemDto;
import com.tbaar.eurder.feature.item.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/catalog")
@Tag(name = "Catalog", description = "What anybody can browse and order: the items, without their stock")
public class CatalogController {

    private final ItemService itemService;

    public CatalogController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List the items of the catalog, sorted by name")
    @ApiResponse(responseCode = "200", description = "The items, without stock nor urgency", useReturnTypeSchema = true)
    public List<CatalogItemDto> findAll() {
        return itemService.findCatalog();
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get one item of the catalog")
    @ApiResponse(responseCode = "200", description = "The item, without stock nor urgency", useReturnTypeSchema = true)
    public CatalogItemDto findById(@PathVariable UUID id) {
        return itemService.findCatalogItem(id);
    }
}
