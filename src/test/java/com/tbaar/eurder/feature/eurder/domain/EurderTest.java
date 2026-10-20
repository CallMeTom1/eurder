package com.tbaar.eurder.feature.eurder.domain;

import com.tbaar.eurder.feature.customer.domain.Address;
import com.tbaar.eurder.feature.customer.domain.Customer;
import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.item.domain.ItemState;
import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.Role;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EurderTest {

    private static final LocalDate ORDER_DATE = LocalDate.of(2026, 12, 31);
    private static final Instant ORDERED_AT = Instant.parse("2026-12-31T09:00:00Z");

    @Test
    void givenSeveralItemGroups_whenComputingTotalPrice_thenReturnsTheExactSumOfTheirSubtotals() {
        Eurder eurder = new Eurder(aCustomer(), List.of(
                ItemGroup.of(anItem("0.10"), ItemState.PRISTINE, 3, 10, ORDER_DATE),
                ItemGroup.of(anItem("49.99"), ItemState.PRISTINE, 2, 10, ORDER_DATE)), ORDERED_AT);

        BigDecimal total = eurder.totalPrice();

        assertThat(total).isEqualTo(new BigDecimal("100.28"));
    }

    @Test
    void givenItemGroups_whenCreatingEurder_thenKeepsThemInTheGivenOrder() {
        ItemGroup chairs = ItemGroup.of(anItem("49.99"), ItemState.PRISTINE, 2, 10, ORDER_DATE);
        ItemGroup tables = ItemGroup.of(anItem("199.00"), ItemState.PRISTINE, 1, 10, ORDER_DATE);

        Eurder eurder = new Eurder(aCustomer(), List.of(chairs, tables), ORDERED_AT);

        assertThat(eurder.getItemGroups()).containsExactly(chairs, tables);
    }

    @Test
    void givenItemGroups_whenCreatingEurder_thenNumbersThemFromOneInTheGivenOrder() {
        List<ItemGroup> itemGroups = List.of(
                ItemGroup.of(anItem("49.99"), ItemState.PRISTINE, 2, 10, ORDER_DATE),
                ItemGroup.of(anItem("199.00"), ItemState.PRISTINE, 1, 10, ORDER_DATE));

        Eurder eurder = new Eurder(aCustomer(), itemGroups, ORDERED_AT);

        assertThat(eurder.getItemGroups()).extracting(ItemGroup::getLineNumber).containsExactly(1, 2);
    }

    private static Item anItem(String price) {
        return new Item("Chair", "Wooden chair", new BigDecimal(price), 10);
    }

    private static Customer aCustomer() {
        return new Customer(new Credentials("jane@doe.be", "hash", Role.CUSTOMER), "Jane", "Doe",
                new Address("Rue Haute", "1", "1000", "Bruxelles"), "0470123456");
    }
}
