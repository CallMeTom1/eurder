package com.tbaar.eurder.feature.item.domain;

import com.tbaar.eurder.feature.item.exception.ItemUnitAlreadySoldException;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "item_unit")
public class ItemUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @Enumerated(EnumType.STRING)
    private ItemState state;

    private UUID itemGroupId;

    protected ItemUnit() {
    }

    public ItemUnit(Item item, ItemState state) {
        this.item = item;
        this.state = state;
    }

    public UUID getId() {
        return id;
    }

    public Item getItem() {
        return item;
    }

    public ItemState getState() {
        return state;
    }

    public UUID getItemGroupId() {
        return itemGroupId;
    }

    public boolean isAvailable() {
        return itemGroupId == null;
    }

    public void assignTo(UUID itemGroupId) {
        this.itemGroupId = itemGroupId;
    }

    public void changeState(ItemState state) {
        if (!isAvailable()) {
            throw new ItemUnitAlreadySoldException(id);
        }
        this.state = state;
    }
}
