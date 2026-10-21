package com.tbaar.eurder.feature.eurder.service;

import com.tbaar.eurder.feature.eurder.domain.ItemGroup;
import com.tbaar.eurder.feature.item.domain.ItemUnit;

import java.util.List;

record Reservation(ItemGroup itemGroup, List<ItemUnit> units) {

    void assignUnits() {
        units.forEach(unit -> unit.assignTo(itemGroup.getId()));
    }
}
