package com.tbaar.eurder.feature.eurder.domain;

import com.tbaar.eurder.feature.customer.domain.Customer;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Document(collection = "eurders")
@CompoundIndex(def = "{'customer.credentialsId': 1, 'orderedAt': 1}")
@CompoundIndex(def = "{'itemGroups.shippingDate': 1}")
public class Eurder {

    @Id
    private UUID id = UUID.randomUUID();

    private EurderCustomer customer;

    private List<ItemGroup> itemGroups = List.of();

    private Instant orderedAt;

    protected Eurder() {
    }

    public Eurder(Customer customer, List<ItemGroup> itemGroups, Instant orderedAt) {
        this.customer = EurderCustomer.from(customer);
        this.orderedAt = orderedAt;
        this.itemGroups = List.copyOf(itemGroups);
    }

    public UUID getId() {
        return id;
    }

    public EurderCustomer getCustomer() {
        return customer;
    }

    public Instant getOrderedAt() {
        return orderedAt;
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
