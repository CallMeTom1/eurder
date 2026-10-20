package com.tbaar.eurder.feature.item.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
class ItemRepositoryIntegrationTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void givenSavedItem_whenReadingItBackFromTheDatabase_thenEveryFieldIsStored() {
        Item saved = itemRepository.saveAndFlush(new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 10));
        entityManager.clear();

        Optional<Item> found = itemRepository.findById(saved.getId());

        assertThat(found).hasValueSatisfying(item -> {
            assertThat(item.getName()).isEqualTo("Chair");
            assertThat(item.getDescription()).isEqualTo("Wooden chair");
            assertThat(item.getPrice()).isEqualTo(new BigDecimal("49.99"));
            assertThat(item.getStock()).isEqualTo(10);
        });
    }

    @Test
    void givenItemWithTheNameOfAnExistingItem_whenSaving_thenBothItemsAreStored() {
        itemRepository.saveAndFlush(new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 10));

        itemRepository.saveAndFlush(new Item("Chair", "Plastic chair", new BigDecimal("19.99"), 5));

        assertThat(itemRepository.count()).isEqualTo(2);
    }

    @Test
    void givenItemsSavedInAnyOrder_whenFindingAllByStock_thenReturnsThemByStockThenName() {
        saveItems();

        List<Item> items = itemRepository.findAllByOrderByStockAscNameAsc();

        assertThat(items)
                .extracting(Item::getName, Item::getStock)
                .containsExactly(tuple("Lamp", 0), tuple("Bed", 4), tuple("Desk", 4), tuple("Chair", 7), tuple("Table", 12));
    }

    @Test
    void givenItemsSavedInAnyOrder_whenFindingByAStockRange_thenReturnsOnlyThatRangeByStockThenName() {
        saveItems();

        List<Item> items = itemRepository.findByStockBetweenOrderByStockAscNameAsc(0, 4);

        assertThat(items)
                .extracting(Item::getName)
                .containsExactly("Lamp", "Bed", "Desk");
    }

    private void saveItems() {
        itemRepository.saveAndFlush(new Item("Table", "Oak table", new BigDecimal("199.00"), 12));
        itemRepository.saveAndFlush(new Item("Desk", "Office desk", new BigDecimal("149.00"), 4));
        itemRepository.saveAndFlush(new Item("Lamp", "Desk lamp", new BigDecimal("29.00"), 0));
        itemRepository.saveAndFlush(new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 7));
        itemRepository.saveAndFlush(new Item("Bed", "Double bed", new BigDecimal("399.00"), 4));
    }
}
