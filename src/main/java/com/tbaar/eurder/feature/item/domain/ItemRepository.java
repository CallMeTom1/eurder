package com.tbaar.eurder.feature.item.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {

    List<Item> findAllByOrderByStockAscNameAsc();

    List<Item> findByStockBetweenOrderByStockAscNameAsc(int minStock, int maxStock);
}
