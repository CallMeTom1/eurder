package com.tbaar.eurder.feature.item.domain;

import org.springframework.data.domain.Limit;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemUnitRepository extends MongoRepository<ItemUnit, UUID> {

    List<ItemUnit> findByItemIdOrderById(UUID itemId);

    Optional<ItemUnit> findByIdAndItemId(UUID id, UUID itemId);

    List<ItemUnit> findByItemIdAndStateAndItemGroupIdIsNull(UUID itemId, ItemState state, Limit limit);

    @Aggregation(pipeline = {
            "{ $match: { itemId: { $in: ?0 }, itemGroupId: null } }",
            "{ $group: { _id: { itemId: '$itemId', state: '$state' }, count: { $sum: 1 } } }",
            "{ $project: { _id: 0, itemId: '$_id.itemId', state: '$_id.state', count: 1 } }"
    })
    List<AvailableUnits> countAvailableUnits(Collection<UUID> itemIds);
}
