package com.tbaar.eurder.feature.item.api;

import com.tbaar.eurder.feature.item.api.dto.ItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemOverviewDto;
import com.tbaar.eurder.feature.item.api.dto.ItemRequestDto;
import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.item.domain.StockUrgency;
import org.springframework.stereotype.Component;

@Component
public class ItemMapper {

    public Item toEntity(ItemRequestDto dto) {
        return new Item(dto.name(), dto.description(), dto.price(), dto.stock());
    }

    public ItemDto toDto(Item item) {
        return new ItemDto(item.getId(), item.getName(), item.getDescription(), item.getPrice(), item.getStock());
    }

    public ItemOverviewDto toOverviewDto(Item item) {
        return new ItemOverviewDto(item.getId(), item.getName(), item.getDescription(), item.getPrice(), item.getStock(),
                StockUrgency.of(item.getStock()));
    }
}
