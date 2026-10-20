package com.tbaar.eurder.feature.item.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import java.util.UUID;

@Entity
@Table(name = "item")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private String description;

    private BigDecimal price;

    @Version
    private long version;

    protected Item() {
    }

    public Item(String name, String description, BigDecimal price) {
        this.name = name;
        this.description = description;
        this.price = twoDecimals(price);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void update(String name, String description, BigDecimal price) {
        this.name = name;
        this.description = description;
        this.price = twoDecimals(price);
    }

    public BigDecimal priceFor(ItemState state) {
        return state.applyTo(price);
    }

    public List<ItemUnit> newUnits(ItemState state, int count) {
        return IntStream.range(0, count)
                .mapToObj(unit -> new ItemUnit(this, state))
                .toList();
    }

    private static BigDecimal twoDecimals(BigDecimal price) {
        return price.setScale(2);
    }
}
