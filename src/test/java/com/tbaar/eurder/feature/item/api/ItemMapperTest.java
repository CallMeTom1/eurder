package com.tbaar.eurder.feature.item.api;

import com.tbaar.eurder.feature.item.api.dto.CreateItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemOverviewDto;
import com.tbaar.eurder.feature.item.domain.AvailableUnits;
import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.item.domain.ItemState;
import com.tbaar.eurder.feature.item.domain.StockUrgency;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class ItemMapperTest {

    private final ItemMapper itemMapper = new ItemMapper();

    @Test
    void givenCreateItemDto_whenMappingToEntity_thenCopiesTheProductFields() {
        CreateItemDto dto = new CreateItemDto("Chair", "Wooden chair", new BigDecimal("49.99"), Map.of(ItemState.PRISTINE, 10));

        Item item = itemMapper.toEntity(dto);

        assertThat(item.getName()).isEqualTo("Chair");
        assertThat(item.getDescription()).isEqualTo("Wooden chair");
        assertThat(item.getPrice()).isEqualTo(new BigDecimal("49.99"));
    }

    @Test
    void givenItemAndItsAvailableUnits_whenMappingToDto_thenListsEveryStateInOrderWithZeroForMissingOnes() {
        Item item = new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 0);
        List<AvailableUnits> counts = List.of(
                new AvailableUnits(item.getId(), ItemState.DAMAGED, 2),
                new AvailableUnits(item.getId(), ItemState.PRISTINE, 8));

        ItemDto dto = itemMapper.toDto(item, counts);

        assertThat(dto.name()).isEqualTo("Chair");
        assertThat(dto.price()).isEqualTo(new BigDecimal("49.99"));
        assertThat(dto.availableUnits()).containsExactly(
                entry(ItemState.PRISTINE, 8L), entry(ItemState.SLIGHTLY_DAMAGED, 0L), entry(ItemState.DAMAGED, 2L));
    }

    @Test
    void givenItem_whenMappingToOverviewDto_thenAddsTheUrgencyOfItsStock() {
        Item item = new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 7);

        ItemOverviewDto dto = itemMapper.toOverviewDto(item);

        assertThat(dto).isEqualTo(new ItemOverviewDto(item.getId(), "Chair", "Wooden chair", new BigDecimal("49.99"), 7, StockUrgency.STOCK_MEDIUM));
    }
}
