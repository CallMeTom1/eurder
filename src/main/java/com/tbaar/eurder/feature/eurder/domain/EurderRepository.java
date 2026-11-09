package com.tbaar.eurder.feature.eurder.domain;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EurderRepository extends MongoRepository<Eurder, UUID> {

    List<Eurder> findAllByCustomerCredentialsIdOrderByOrderedAtAscIdAsc(UUID credentialsId);

    Optional<Eurder> findByIdAndCustomerCredentialsId(UUID id, UUID credentialsId);

    @Aggregation(pipeline = {
            "{ $match: { 'itemGroups.shippingDate': ?0 } }",
            "{ $sort: { orderedAt: 1, _id: 1 } }",
            "{ $unwind: '$itemGroups' }",
            "{ $match: { 'itemGroups.shippingDate': ?0 } }",
            "{ $project: { _id: 0, eurderId: '$_id', firstName: '$customer.firstName', lastName: '$customer.lastName', address: '$customer.address', itemName: '$itemGroups.itemName', itemState: '$itemGroups.itemState', amount: '$itemGroups.amount' } }"
    })
    List<ItemGroupToShip> findItemGroupsShippingOn(LocalDate shippingDate);
}
