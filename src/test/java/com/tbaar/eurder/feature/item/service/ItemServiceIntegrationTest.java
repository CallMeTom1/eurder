package com.tbaar.eurder.feature.item.service;

import com.tbaar.eurder.feature.item.api.dto.CreateItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemDto;
import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.item.domain.ItemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ItemServiceIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @AfterEach
    void deleteCreatedItems() {
        itemRepository.deleteAll();
    }

    @Test
    void givenValidItem_whenCreating_thenTheItemIsStoredUnderTheReturnedId() {
        CreateItemDto request = new CreateItemDto("Chair", "Wooden chair", new BigDecimal("49.9"), 0);

        ItemDto created = itemService.create(request);

        Optional<Item> stored = itemRepository.findById(created.id());
        assertThat(stored).hasValueSatisfying(item -> {
            assertThat(item.getName()).isEqualTo("Chair");
            assertThat(item.getPrice()).isEqualTo(new BigDecimal("49.90"));
            assertThat(item.getStock()).isZero();
        });
    }
}
