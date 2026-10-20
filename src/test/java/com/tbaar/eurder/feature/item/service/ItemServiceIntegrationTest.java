package com.tbaar.eurder.feature.item.service;

import com.tbaar.eurder.feature.item.api.dto.CreateItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemRequestDto;
import com.tbaar.eurder.feature.item.domain.AvailableUnits;
import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.item.domain.ItemRepository;
import com.tbaar.eurder.feature.item.domain.ItemState;
import com.tbaar.eurder.feature.item.domain.ItemUnitRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
class ItemServiceIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemUnitRepository itemUnitRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void deleteCreatedItems() {
        itemUnitRepository.deleteAll();
        itemRepository.deleteAll();
    }

    @Test
    void givenValidItemWithUnits_whenCreating_thenTheItemAndItsAvailableUnitsAreStored() {
        CreateItemDto request = new CreateItemDto("Chair", "Wooden chair", new BigDecimal("49.9"),
                Map.of(ItemState.PRISTINE, 2, ItemState.DAMAGED, 1));

        ItemDto created = itemService.create(request);

        Optional<Item> stored = itemRepository.findById(created.id());
        assertThat(stored).hasValueSatisfying(item -> {
            assertThat(item.getName()).isEqualTo("Chair");
            assertThat(item.getPrice()).isEqualTo(new BigDecimal("49.90"));
        });
        assertThat(itemUnitRepository.countAvailableUnits(List.of(created.id())))
                .extracting(AvailableUnits::state, AvailableUnits::count)
                .containsExactlyInAnyOrder(tuple(ItemState.PRISTINE, 2L), tuple(ItemState.DAMAGED, 1L));
    }

    @Test
    void givenExistingItem_whenUpdating_thenTheNewValuesAreStoredWithoutAnExplicitSave() {
        Item chair = itemRepository.save(new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 10));

        itemService.update(chair.getId(), new ItemRequestDto("Armchair", "Leather armchair", new BigDecimal("150"), 3));

        assertThat(itemRepository.findById(chair.getId())).hasValueSatisfying(item -> {
            assertThat(item.getName()).isEqualTo("Armchair");
            assertThat(item.getDescription()).isEqualTo("Leather armchair");
            assertThat(item.getPrice()).isEqualTo(new BigDecimal("150.00"));
            assertThat(item.getStock()).isEqualTo(3);
        });
    }

    @Test
    void givenTheStockChangedByAnotherTransactionMeanwhile_whenTheUpdateCommits_thenFailsAndKeepsTheOtherChange() {
        Item chair = itemRepository.save(new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 10));
        TransactionTemplate updateTransaction = new TransactionTemplate(transactionManager);
        TransactionTemplate concurrentTransaction = new TransactionTemplate(transactionManager);
        concurrentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Throwable thrown = catchThrowable(() -> updateTransaction.executeWithoutResult(updateStatus -> {
            itemService.update(chair.getId(), new ItemRequestDto("Chair", "Wooden chair", new BigDecimal("39.99"), 20));
            concurrentTransaction.executeWithoutResult(concurrentStatus ->
                    itemRepository.findById(chair.getId()).orElseThrow().removeFromStock(4));
        }));

        assertThat(thrown).isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(itemRepository.findById(chair.getId())).hasValueSatisfying(item -> {
            assertThat(item.getPrice()).isEqualTo(new BigDecimal("49.99"));
            assertThat(item.getStock()).isEqualTo(6);
        });
    }
}
