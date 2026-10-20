package com.tbaar.eurder.feature.eurder.domain;

import com.tbaar.eurder.feature.customer.domain.Customer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Eurder {

    private UUID id;

    private Customer customer;

    private List<ItemGroup> itemGroups = new ArrayList<>();

    protected Eurder() {
    }

    public Eurder(Customer customer, List<ItemGroup> itemGroups) {
        this.customer = customer;
        this.itemGroups = new ArrayList<>(itemGroups);
    }

    public UUID getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public List<ItemGroup> getItemGroups() {
        return List.copyOf(itemGroups);
    }

    public BigDecimal totalPrice() {
        return itemGroups.stream()
                .map(ItemGroup::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
