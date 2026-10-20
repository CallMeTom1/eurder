package com.tbaar.eurder.feature.item.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ItemTest {

    @ParameterizedTest(name = "price {0} is stored as {1}")
    @CsvSource({"10, 10.00", "9.9, 9.90", "49.99, 49.99"})
    void givenPriceWithAtMostTwoDecimals_whenCreatingItem_thenThePriceHasExactlyTwoDecimals(String price, String expectedPrice) {
        Item item = new Item("Chair", "Wooden chair", new BigDecimal(price), 10);

        assertThat(item.getPrice()).isEqualTo(new BigDecimal(expectedPrice));
    }

    @ParameterizedTest(name = "stock {0}, amount {1}: in stock = {2}")
    @CsvSource({"10, 2, true", "2, 2, true", "1, 2, false", "0, 1, false"})
    void givenStockAndAmount_whenCheckingIfInStock_thenTheStockMustCoverTheWholeAmount(int stock, int amount, boolean expected) {
        Item item = new Item("Chair", "Wooden chair", new BigDecimal("49.99"), stock);

        boolean inStock = item.isInStock(amount);

        assertThat(inStock).isEqualTo(expected);
    }

    @ParameterizedTest(name = "stock {0} minus {1} leaves {2}")
    @CsvSource({"10, 2, 8", "2, 2, 0", "5, 10, 0", "0, 3, 0"})
    void givenStockAndAmount_whenRemovingFromStock_thenTheStockDecreasesButNeverBelowZero(int stock, int amount, int expectedStock) {
        Item item = new Item("Chair", "Wooden chair", new BigDecimal("49.99"), stock);

        item.removeFromStock(amount);

        assertThat(item.getStock()).isEqualTo(expectedStock);
    }

    @Test
    void givenItem_whenUpdatingIt_thenReplacesEveryFieldWithATwoDecimalPrice() {
        Item item = new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 10);

        item.update("Armchair", "Leather armchair", new BigDecimal("150"), 3);

        assertThat(item)
                .extracting(Item::getName, Item::getDescription, Item::getPrice, Item::getStock)
                .containsExactly("Armchair", "Leather armchair", new BigDecimal("150.00"), 3);
    }
}
