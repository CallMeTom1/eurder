package com.tbaar.eurder.feature.item.service;

import com.tbaar.eurder.feature.item.api.ItemMapper;
import com.tbaar.eurder.feature.item.api.dto.ItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemOverviewDto;
import com.tbaar.eurder.feature.item.api.dto.ItemRequestDto;
import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.item.domain.ItemRepository;
import com.tbaar.eurder.feature.item.domain.StockUrgency;
import com.tbaar.eurder.feature.item.exception.ItemNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
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

    @Test
    void givenExistingItem_whenUpdating_thenReplacesItsFieldsAndReturnsTheUpdatedItem() {
        UUID id = UUID.randomUUID();
        Item chair = new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 10);
        when(itemRepository.findById(id)).thenReturn(Optional.of(chair));

        ItemDto updated = itemService.update(id, new ItemRequestDto("Armchair", "Leather armchair", new BigDecimal("150"), 3));

        assertThat(updated)
                .extracting(ItemDto::name, ItemDto::description, ItemDto::price, ItemDto::stock)
                .containsExactly("Armchair", "Leather armchair", new BigDecimal("150.00"), 3);
        assertThat(chair.getName()).isEqualTo("Armchair");
    }

    @Test
    void givenUnknownItemId_whenUpdating_thenThrowsItemNotFoundWithTheId() {
        UUID unknownId = UUID.randomUUID();
        when(itemRepository.findById(unknownId)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> itemService.update(unknownId, new ItemRequestDto("Armchair", "Leather armchair", new BigDecimal("150"), 3)));

        assertThat(thrown)
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessage("No item found for id " + unknownId);
    }

    @Test
    void givenNoUrgencyFilter_whenFindingTheOverview_thenReturnsEveryItemSortedWithItsUrgency() {
        when(itemRepository.findAllByOrderByStockAscNameAsc()).thenReturn(List.of(
                new Item("Lamp", "Desk lamp", new BigDecimal("29.00"), 0),
                new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 7),
                new Item("Table", "Oak table", new BigDecimal("199.00"), 12)));

        List<ItemOverviewDto> overview = itemService.findOverview(null);

        assertThat(overview)
                .extracting(ItemOverviewDto::name, ItemOverviewDto::urgency)
                .containsExactly(
                        tuple("Lamp", StockUrgency.STOCK_LOW),
                        tuple("Chair", StockUrgency.STOCK_MEDIUM),
                        tuple("Table", StockUrgency.STOCK_HIGH));
    }

    @Test
    void givenAnUrgencyFilter_whenFindingTheOverview_thenQueriesTheStockRangeOfThatUrgency() {
        when(itemRepository.findByStockBetweenOrderByStockAscNameAsc(5, 9)).thenReturn(List.of(
                new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 7)));

        List<ItemOverviewDto> overview = itemService.findOverview(StockUrgency.STOCK_MEDIUM);

        assertThat(overview).extracting(ItemOverviewDto::name).containsExactly("Chair");
    }
}
