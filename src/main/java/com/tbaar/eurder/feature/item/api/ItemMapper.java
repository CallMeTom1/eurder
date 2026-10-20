package com.tbaar.eurder.feature.item.api;

import com.tbaar.eurder.feature.item.api.dto.CreateItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemOverviewDto;
import com.tbaar.eurder.feature.item.api.dto.ItemUnitDto;
import com.tbaar.eurder.feature.item.domain.AvailableUnits;
import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.item.domain.ItemState;
import com.tbaar.eurder.feature.item.domain.ItemStock;
import com.tbaar.eurder.feature.item.domain.ItemUnit;
import com.tbaar.eurder.feature.item.domain.StockUrgency;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ItemMapper {

    public Item toEntity(CreateItemDto dto) {
        int totalUnits = dto.units().values().stream().mapToInt(Integer::intValue).sum();
        return new Item(dto.name(), dto.description(), dto.price(), totalUnits);
    }

    public ItemDto toDto(Item item, List<AvailableUnits> availableUnits) {
        return new ItemDto(item.getId(), item.getName(), item.getDescription(), item.getPrice(), byState(availableUnits));
    }

    public ItemOverviewDto toOverviewDto(ItemStock stock, List<AvailableUnits> availableUnits) {
        Item item = stock.item();
        return new ItemOverviewDto(item.getId(), item.getName(), item.getDescription(), item.getPrice(), byState(availableUnits),
                StockUrgency.of(Math.toIntExact(stock.availableUnits())));
    }

    public ItemUnitDto toUnitDto(ItemUnit unit) {
        return new ItemUnitDto(unit.getId(), unit.getState(), unit.isAvailable());
    }

    private static Map<ItemState, Long> byState(List<AvailableUnits> availableUnits) {
        Map<ItemState, Long> byState = new EnumMap<>(ItemState.class);
        for (ItemState state : ItemState.values()) {
            byState.put(state, 0L);
        }
        availableUnits.forEach(units -> byState.put(units.state(), units.count()));
        return byState;
    }
}
