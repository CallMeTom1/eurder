package com.tbaar.eurder.feature.item.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    Optional<Item> findLockedById(UUID id);

    @Query("""
            select new com.tbaar.eurder.feature.item.domain.ItemStock(i, count(u))
            from Item i
            left join ItemUnit u on u.item = i and u.itemGroupId is null
            group by i
            having count(u) between :minUnits and :maxUnits
            order by count(u), i.name
            """)
    List<ItemStock> findStockBetween(@Param("minUnits") long minUnits, @Param("maxUnits") long maxUnits);
}
