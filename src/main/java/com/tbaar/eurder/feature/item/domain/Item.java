package com.tbaar.eurder.feature.item.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
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

    private int stock;

    @Version
    private long version;

    protected Item() {
    }

    public Item(String name, String description, BigDecimal price, int stock) {
        this.name = name;
        this.description = description;
        this.price = price.setScale(2);
        this.stock = stock;
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

    public int getStock() {
        return stock;
    }

    public boolean isInStock(int amount) {
        return stock >= amount;
    }

    public void removeFromStock(int amount) {
        stock = Math.max(0, stock - amount);
    }
}
