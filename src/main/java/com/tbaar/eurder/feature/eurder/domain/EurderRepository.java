package com.tbaar.eurder.feature.eurder.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EurderRepository extends JpaRepository<Eurder, UUID> {

    @EntityGraph(attributePaths = "itemGroups")
    List<Eurder> findAllByCustomerCredentialsIdOrderByOrderedAtAscIdAsc(UUID credentialsId);

    @EntityGraph(attributePaths = "itemGroups")
    Optional<Eurder> findByIdAndCustomerCredentialsId(UUID id, UUID credentialsId);

    @Query("""
            select new com.tbaar.eurder.feature.eurder.domain.ItemGroupToShip(
                e.id, c.firstName, c.lastName, c.address, g.itemName, g.amount)
            from Eurder e
            join e.customer c
            join e.itemGroups g
            where g.shippingDate = :shippingDate
            order by e.orderedAt, e.id, g.lineNumber
            """)
    List<ItemGroupToShip> findItemGroupsShippingOn(@Param("shippingDate") LocalDate shippingDate);
}
