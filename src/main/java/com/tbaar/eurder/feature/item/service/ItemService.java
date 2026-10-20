package com.tbaar.eurder.feature.item.service;

import com.tbaar.eurder.feature.item.api.ItemMapper;
import com.tbaar.eurder.feature.item.api.dto.AddUnitsDto;
import com.tbaar.eurder.feature.item.api.dto.ChangeUnitStateDto;
import com.tbaar.eurder.feature.item.api.dto.CreateItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemOverviewDto;
import com.tbaar.eurder.feature.item.api.dto.UpdateItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemUnitDto;
import com.tbaar.eurder.feature.item.domain.AvailableUnits;
import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.item.domain.ItemRepository;
import com.tbaar.eurder.feature.item.domain.ItemStock;
import com.tbaar.eurder.feature.item.domain.ItemUnit;
import com.tbaar.eurder.feature.item.domain.ItemUnitRepository;
import com.tbaar.eurder.feature.item.domain.StockUrgency;
import com.tbaar.eurder.feature.item.exception.ItemNotFoundException;
import com.tbaar.eurder.feature.item.exception.ItemUnitNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemUnitRepository itemUnitRepository;
    private final ItemMapper itemMapper;

    public ItemService(ItemRepository itemRepository, ItemUnitRepository itemUnitRepository, ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.itemUnitRepository = itemUnitRepository;
        this.itemMapper = itemMapper;
    }

    public ItemDto create(CreateItemDto request) {
        Item item = itemRepository.save(itemMapper.toEntity(request));
        List<ItemUnit> units = request.units().entrySet().stream()
                .flatMap(unitsOfAState -> item.newUnits(unitsOfAState.getKey(), unitsOfAState.getValue()).stream())
                .toList();
        itemUnitRepository.saveAll(units);
        List<AvailableUnits> availableUnits = request.units().entrySet().stream()
                .map(unitsOfAState -> new AvailableUnits(item.getId(), unitsOfAState.getKey(), unitsOfAState.getValue()))
                .toList();
        return itemMapper.toDto(item, availableUnits);
    }

    public ItemDto update(UUID id, UpdateItemDto request) {
        Item item = itemRepository.findById(id).orElseThrow(() -> new ItemNotFoundException(id));
        item.update(request.name(), request.description(), request.price());
        return itemMapper.toDto(item, itemUnitRepository.countAvailableUnits(List.of(id)));
    }

    @Transactional(readOnly = true)
    public List<ItemOverviewDto> findOverview(StockUrgency urgency) {
        List<ItemStock> stock = urgency == null
                ? itemRepository.findStockBetween(0, Integer.MAX_VALUE)
                : itemRepository.findStockBetween(urgency.getMinStock(), urgency.getMaxStock());
        if (stock.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<AvailableUnits>> availableUnitsByItem = itemUnitRepository
                .countAvailableUnits(stock.stream().map(itemStock -> itemStock.item().getId()).toList()).stream()
                .collect(Collectors.groupingBy(AvailableUnits::itemId));
        return stock.stream()
                .map(itemStock -> itemMapper.toOverviewDto(itemStock, availableUnitsByItem.getOrDefault(itemStock.item().getId(), List.of())))
                .toList();
    }

    public ItemDto addUnits(UUID itemId, AddUnitsDto request) {
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new ItemNotFoundException(itemId));
        itemUnitRepository.saveAll(item.newUnits(request.state(), request.amount()));
        return itemMapper.toDto(item, itemUnitRepository.countAvailableUnits(List.of(itemId)));
    }

    public ItemUnitDto changeUnitState(UUID itemId, UUID unitId, ChangeUnitStateDto request) {
        itemRepository.findLockedById(itemId).orElseThrow(() -> new ItemNotFoundException(itemId));
        ItemUnit unit = itemUnitRepository.findByIdAndItemId(unitId, itemId)
                .orElseThrow(() -> new ItemUnitNotFoundException(itemId, unitId));
        unit.changeState(request.state());
        return itemMapper.toUnitDto(unit);
    }

    @Transactional(readOnly = true)
    public List<ItemUnitDto> findUnits(UUID itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new ItemNotFoundException(itemId);
        }
        return itemUnitRepository.findByItemIdOrderById(itemId).stream()
                .sorted(Comparator.comparing(ItemUnit::getState).thenComparing(unit -> !unit.isAvailable()))
                .map(itemMapper::toUnitDto)
                .toList();
    }
}
