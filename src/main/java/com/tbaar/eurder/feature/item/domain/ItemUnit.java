package com.tbaar.eurder.feature.item.domain;

import com.tbaar.eurder.feature.item.exception.ItemUnitAlreadySoldException;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "item_units")
@CompoundIndex(def = "{'itemId': 1, 'state': 1, 'itemGroupId': 1}")
public class ItemUnit {

    @Id
    private UUID id = UUID.randomUUID();

    private UUID itemId;

    private ItemState state;

    private UUID itemGroupId;

    protected ItemUnit() {
    }

    public ItemUnit(Item item, ItemState state) {
        this.itemId = item.getId();
        this.state = state;
    }

    public UUID getId() {
        return id;
    }

    public UUID getItemId() {
        return itemId;
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
