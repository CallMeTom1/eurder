package com.tbaar.eurder.feature.item.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
}
