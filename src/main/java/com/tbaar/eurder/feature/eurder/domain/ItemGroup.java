package com.tbaar.eurder.feature.eurder.domain;

import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.item.domain.ItemState;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "item_group")
public class ItemGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private int lineNumber;

    private UUID itemId;

    private String itemName;

    @Enumerated(EnumType.STRING)
    private ItemState itemState;

    private BigDecimal itemPrice;

    private int amount;

    private LocalDate shippingDate;

    protected ItemGroup() {
    }

    private ItemGroup(UUID itemId, String itemName, ItemState itemState, BigDecimal itemPrice, int amount, LocalDate shippingDate) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemState = itemState;
        this.itemPrice = itemPrice;
        this.amount = amount;
        this.shippingDate = shippingDate;
    }

    public static ItemGroup of(Item item, ItemState state, int amount, int availableUnits, LocalDate orderDate) {
        LocalDate shippingDate = availableUnits >= amount ? orderDate.plusDays(1) : orderDate.plusWeeks(1);
        return new ItemGroup(item.getId(), item.getName(), state, item.priceFor(state), amount, shippingDate);
    }

    public UUID getId() {
        return id;
    }

    void numberAs(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public UUID getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public ItemState getItemState() {
        return itemState;
    }

    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    public int getAmount() {
        return amount;
    }

    public LocalDate getShippingDate() {
        return shippingDate;
    }

    public BigDecimal subtotal() {
        return itemPrice.multiply(BigDecimal.valueOf(amount));
    }
}
