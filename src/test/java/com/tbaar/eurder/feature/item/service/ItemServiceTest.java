package com.tbaar.eurder.feature.item.service;

import com.tbaar.eurder.feature.item.api.ItemMapper;
import com.tbaar.eurder.feature.item.api.dto.CreateItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemOverviewDto;
import com.tbaar.eurder.feature.item.api.dto.ItemRequestDto;
import com.tbaar.eurder.feature.item.domain.AvailableUnits;
import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.item.domain.ItemRepository;
import com.tbaar.eurder.feature.item.domain.ItemState;
import com.tbaar.eurder.feature.item.domain.ItemUnit;
import com.tbaar.eurder.feature.item.domain.ItemUnitRepository;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemUnitRepository itemUnitRepository;

    private ItemService itemService;

    @BeforeEach
    void createService() {
        itemService = new ItemService(itemRepository, itemUnitRepository, new ItemMapper());
    }

    @Test
    void givenItemWithUnitsInTwoStates_whenCreating_thenSavesTheItemAndOneUnitPerPieceInItsState() {
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        itemService.create(new CreateItemDto("Chair", "Wooden chair", new BigDecimal("49.99"),
                Map.of(ItemState.PRISTINE, 2, ItemState.DAMAGED, 1)));

        ArgumentCaptor<Item> savedItem = ArgumentCaptor.forClass(Item.class);
        verify(itemRepository).save(savedItem.capture());
        assertThat(savedItem.getValue().getName()).isEqualTo("Chair");
        ArgumentCaptor<List<ItemUnit>> savedUnits = ArgumentCaptor.captor();
        verify(itemUnitRepository).saveAll(savedUnits.capture());
        assertThat(savedUnits.getValue())
                .extracting(ItemUnit::getState)
                .containsExactlyInAnyOrder(ItemState.PRISTINE, ItemState.PRISTINE, ItemState.DAMAGED);
    }

    @Test
    void givenValidItem_whenCreating_thenReturnsItWithATwoDecimalPriceAndItsAvailableUnitsPerState() {
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ItemDto created = itemService.create(new CreateItemDto("Chair", "Wooden chair", new BigDecimal("49"),
                Map.of(ItemState.PRISTINE, 2, ItemState.DAMAGED, 1)));

        assertThat(created.name()).isEqualTo("Chair");
        assertThat(created.price()).isEqualTo(new BigDecimal("49.00"));
        assertThat(created.availableUnits()).containsExactly(
                entry(ItemState.PRISTINE, 2L), entry(ItemState.SLIGHTLY_DAMAGED, 0L), entry(ItemState.DAMAGED, 1L));
    }

    @Test
    void givenExistingItem_whenUpdating_thenReplacesItsFieldsAndReturnsItWithItsAvailableUnits() {
        UUID id = UUID.randomUUID();
        Item chair = new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 10);
        when(itemRepository.findById(id)).thenReturn(Optional.of(chair));
        when(itemUnitRepository.countAvailableUnits(List.of(id))).thenReturn(List.of(new AvailableUnits(id, ItemState.PRISTINE, 10)));

        ItemDto updated = itemService.update(id, new ItemRequestDto("Armchair", "Leather armchair", new BigDecimal("150"), 3));

        assertThat(updated)
                .extracting(ItemDto::name, ItemDto::description, ItemDto::price)
                .containsExactly("Armchair", "Leather armchair", new BigDecimal("150.00"));
        assertThat(updated.availableUnits()).containsEntry(ItemState.PRISTINE, 10L);
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
