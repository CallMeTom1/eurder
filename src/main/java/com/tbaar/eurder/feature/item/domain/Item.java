package com.tbaar.eurder.feature.item.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import java.util.UUID;

@Document(collection = "items")
public class Item {

    @Id
    private UUID id = UUID.randomUUID();

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
