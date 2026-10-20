package com.tbaar.eurder.feature.item.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ItemStateTest {

    @ParameterizedTest(name = "{0} at {1} costs {2}")
    @CsvSource({
            "PRISTINE, 49.99, 49.99",
            "SLIGHTLY_DAMAGED, 49.99, 47.49",
            "DAMAGED, 49.99, 44.99",
            "SLIGHTLY_DAMAGED, 10.00, 9.50",
            "DAMAGED, 0.05, 0.05",
            "SLIGHTLY_DAMAGED, 0.10, 0.10"})
    void givenStateAndBasePrice_whenApplyingTheDiscount_thenReturnsThePriceRoundedHalfUpToTheCent(ItemState state, String basePrice, String expectedPrice) {
        BigDecimal price = state.applyTo(new BigDecimal(basePrice));

        assertThat(price).isEqualTo(new BigDecimal(expectedPrice));
    }
}
