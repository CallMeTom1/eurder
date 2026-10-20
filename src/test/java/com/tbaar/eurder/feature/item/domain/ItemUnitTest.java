package com.tbaar.eurder.feature.item.domain;

import com.tbaar.eurder.feature.item.exception.ItemUnitAlreadySoldException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ItemUnitTest {

    private final Item chair = new Item("Chair", "Wooden chair", new BigDecimal("49.99"));

    @Test
    void givenAvailableUnit_whenChangingItsState_thenItHasTheNewState() {
        ItemUnit unit = new ItemUnit(chair, ItemState.PRISTINE);

        unit.changeState(ItemState.DAMAGED);

        assertThat(unit.getState()).isEqualTo(ItemState.DAMAGED);
    }

    @Test
    void givenSoldUnit_whenChangingItsState_thenRefusesAndKeepsTheStateItWasSoldIn() {
        ItemUnit unit = new ItemUnit(chair, ItemState.PRISTINE);
        unit.assignTo(UUID.randomUUID());

        Throwable thrown = catchThrowable(() -> unit.changeState(ItemState.DAMAGED));

        assertThat(thrown)
                .isInstanceOf(ItemUnitAlreadySoldException.class)
                .hasMessage("Item unit " + unit.getId() + " is already sold");
        assertThat(unit.getState()).isEqualTo(ItemState.PRISTINE);
    }
}
