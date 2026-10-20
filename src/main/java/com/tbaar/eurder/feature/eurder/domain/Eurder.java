package com.tbaar.eurder.feature.eurder.domain;

import com.tbaar.eurder.feature.customer.domain.Customer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "eurder")
public class Eurder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "eurder_id", nullable = false, updatable = false)
    @OrderBy("lineNumber")
    private List<ItemGroup> itemGroups = new ArrayList<>();

    protected Eurder() {
    }

    public Eurder(Customer customer, List<ItemGroup> itemGroups) {
        this.customer = customer;
        this.itemGroups = new ArrayList<>(itemGroups);
        for (int index = 0; index < this.itemGroups.size(); index++) {
            this.itemGroups.get(index).numberAs(index + 1);
        }
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
