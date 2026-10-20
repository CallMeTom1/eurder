package com.tbaar.eurder.feature.item.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {

    List<Item> findAllByOrderByStockAscNameAsc();

    List<Item> findByStockBetweenOrderByStockAscNameAsc(int minStock, int maxStock);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    Optional<Item> findLockedById(UUID id);
}
