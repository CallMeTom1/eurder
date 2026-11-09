package com.tbaar.eurder.feature.item.domain;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends MongoRepository<Item, UUID> {

    default Optional<Item> findLockedById(UUID id) {
        return findById(id).map(this::save);
    }

    @Aggregation(pipeline = {
            "{ $lookup: { from: 'item_units', localField: '_id', foreignField: 'itemId', pipeline: [ { $match: { itemGroupId: null } }, { $project: { _id: 1 } } ], as: 'availableUnits' } }",
            "{ $addFields: { availableUnits: { $size: '$availableUnits' } } }",
            "{ $match: { availableUnits: { $gte: ?0, $lte: ?1 } } }",
            "{ $sort: { availableUnits: 1, name: 1 } }",
            "{ $project: { _id: 0, availableUnits: 1, item: { _id: '$_id', name: '$name', description: '$description', price: '$price', version: '$version' } } }"
    })
    List<ItemStock> findStockBetween(long minUnits, long maxUnits);
}
