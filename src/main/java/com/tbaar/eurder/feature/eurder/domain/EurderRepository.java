package com.tbaar.eurder.feature.eurder.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EurderRepository extends JpaRepository<Eurder, UUID> {

    @EntityGraph(attributePaths = "itemGroups")
    List<Eurder> findAllByCustomerCredentialsIdOrderByOrderedAtAscIdAsc(UUID credentialsId);
}
