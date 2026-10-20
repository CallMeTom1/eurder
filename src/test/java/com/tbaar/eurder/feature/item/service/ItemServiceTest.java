package com.tbaar.eurder.feature.item.service;

import com.tbaar.eurder.feature.item.api.ItemMapper;
import com.tbaar.eurder.feature.item.api.dto.ItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemRequestDto;
import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.item.domain.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    private ItemService itemService;

    @BeforeEach
    void createService() {
        itemService = new ItemService(itemRepository, new ItemMapper());
    }

    @Test
    void givenValidItem_whenCreating_thenSavesTheItem() {
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        itemService.create(new ItemRequestDto("Chair", "Wooden chair", new BigDecimal("49.99"), 10));

        ArgumentCaptor<Item> saved = ArgumentCaptor.forClass(Item.class);
        verify(itemRepository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Chair");
        assertThat(saved.getValue().getStock()).isEqualTo(10);
    }

    @Test
    void givenValidItem_whenCreating_thenReturnsTheSavedItemWithATwoDecimalPrice() {
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ItemDto created = itemService.create(new ItemRequestDto("Chair", "Wooden chair", new BigDecimal("49"), 10));

        assertThat(created.name()).isEqualTo("Chair");
        assertThat(created.description()).isEqualTo("Wooden chair");
        assertThat(created.price()).isEqualTo(new BigDecimal("49.00"));
        assertThat(created.stock()).isEqualTo(10);
    }
}
