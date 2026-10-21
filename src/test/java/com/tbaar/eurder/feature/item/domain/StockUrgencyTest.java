package com.tbaar.eurder.feature.item.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class StockUrgencyTest {

    @ParameterizedTest(name = "stock {0} is {1}")
    @CsvSource({"0, STOCK_LOW", "4, STOCK_LOW", "5, STOCK_MEDIUM", "9, STOCK_MEDIUM", "10, STOCK_HIGH", "1000, STOCK_HIGH"})
    void givenStock_whenComputingTheUrgency_thenReturnsTheLevelOfItsRange(int stock, StockUrgency expected) {
        StockUrgency urgency = StockUrgency.from(stock);

        assertThat(urgency).isEqualTo(expected);
    }

    @Test
    void givenNegativeStock_whenComputingTheUrgency_thenThrowsIllegalArgument() {
        int negativeStock = -1;

        Throwable thrown = catchThrowable(() -> StockUrgency.from(negativeStock));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Stock must not be negative: -1");
    }
}
