package com.tbaar.eurder.feature.eurder.domain;

import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.item.domain.ItemState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ItemGroupTest {

    private static final LocalDate NEW_YEARS_EVE = LocalDate.of(2026, 12, 31);

    @ParameterizedTest(name = "{0} available, amount {1}: ships on {2}")
    @CsvSource({"10, 2, 2027-01-01", "2, 2, 2027-01-01", "1, 2, 2027-01-07", "0, 1, 2027-01-07"})
    void givenAvailableUnitsAndAmount_whenCreatingItemGroup_thenShipsNextDayIfEnoughUnitsOtherwiseNextWeek(int availableUnits, int amount, LocalDate expectedShippingDate) {
        Item chair = new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 0);

        ItemGroup itemGroup = ItemGroup.of(chair, ItemState.PRISTINE, amount, availableUnits, NEW_YEARS_EVE);

        assertThat(itemGroup.getShippingDate()).isEqualTo(expectedShippingDate);
    }

    @Test
    void givenItem_whenCreatingItemGroupOfAState_thenCopiesTheNameTheStateAndTheDiscountedPriceWithTheAmount() {
        Item chair = new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 0);

        ItemGroup itemGroup = ItemGroup.of(chair, ItemState.DAMAGED, 2, 5, NEW_YEARS_EVE);

        assertThat(itemGroup.getItemName()).isEqualTo("Chair");
        assertThat(itemGroup.getItemState()).isEqualTo(ItemState.DAMAGED);
        assertThat(itemGroup.getItemPrice()).isEqualTo(new BigDecimal("44.99"));
        assertThat(itemGroup.getAmount()).isEqualTo(2);
    }

    @ParameterizedTest(name = "{0} x {1} = {2}")
    @CsvSource({"0.10, 3, 0.30", "49.99, 2, 99.98", "19.90, 1, 19.90"})
    void givenPriceAndAmount_whenComputingSubtotal_thenReturnsTheExactProduct(String price, int amount, String expectedSubtotal) {
        ItemGroup itemGroup = ItemGroup.of(new Item("Chair", "Wooden chair", new BigDecimal(price), 0), ItemState.PRISTINE, amount, 10, NEW_YEARS_EVE);

        BigDecimal subtotal = itemGroup.subtotal();

        assertThat(subtotal).isEqualTo(new BigDecimal(expectedSubtotal));
    }
}
