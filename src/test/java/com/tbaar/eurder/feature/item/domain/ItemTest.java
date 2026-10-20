package com.tbaar.eurder.feature.item.domain;

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
}
