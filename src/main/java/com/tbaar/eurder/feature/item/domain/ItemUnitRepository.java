package com.tbaar.eurder.feature.item.domain;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemUnitRepository extends JpaRepository<ItemUnit, UUID> {

    List<ItemUnit> findByItemIdOrderById(UUID itemId);

    Optional<ItemUnit> findByIdAndItemId(UUID id, UUID itemId);

    List<ItemUnit> findByItemIdAndStateAndItemGroupIdIsNull(UUID itemId, ItemState state, Limit limit);

    @Query("""
            select new com.tbaar.eurder.feature.item.domain.AvailableUnits(u.item.id, u.state, count(u))
            from ItemUnit u
            where u.itemGroupId is null and u.item.id in :itemIds
            group by u.item.id, u.state
            """)
    List<AvailableUnits> countAvailableUnits(@Param("itemIds") Collection<UUID> itemIds);
}
