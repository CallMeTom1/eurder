package com.tbaar.eurder.feature.item.api;

import com.tbaar.eurder.feature.item.api.dto.ItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemRequestDto;
import com.tbaar.eurder.feature.item.domain.Item;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ItemMapperTest {

    private final ItemMapper itemMapper = new ItemMapper();

    @Test
    void givenItemRequestDto_whenMappingToEntity_thenCopiesEveryField() {
        ItemRequestDto dto = new ItemRequestDto("Chair", "Wooden chair", new BigDecimal("49.99"), 10);

        Item item = itemMapper.toEntity(dto);

        assertThat(item.getName()).isEqualTo("Chair");
        assertThat(item.getDescription()).isEqualTo("Wooden chair");
        assertThat(item.getPrice()).isEqualTo(new BigDecimal("49.99"));
        assertThat(item.getStock()).isEqualTo(10);
    }

    @Test
    void givenItem_whenMappingToDto_thenExposesEveryField() {
        Item item = new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 10);

        ItemDto dto = itemMapper.toDto(item);

        assertThat(dto).isEqualTo(new ItemDto(item.getId(), "Chair", "Wooden chair", new BigDecimal("49.99"), 10));
    }
}
