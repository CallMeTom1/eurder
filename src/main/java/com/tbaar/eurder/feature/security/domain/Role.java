package com.tbaar.eurder.feature.security.domain;

import java.util.Set;

import static com.tbaar.eurder.feature.security.domain.Feature.MANAGE_ITEMS;
import static com.tbaar.eurder.feature.security.domain.Feature.ORDER_ITEMS;
import static com.tbaar.eurder.feature.security.domain.Feature.VIEW_CUSTOMERS;
import static com.tbaar.eurder.feature.security.domain.Feature.VIEW_ITEM_OVERVIEW;
import static com.tbaar.eurder.feature.security.domain.Feature.VIEW_OWN_EURDERS;
import static com.tbaar.eurder.feature.security.domain.Feature.VIEW_SHIPPING_TODAY;

public enum Role {
    ADMIN(VIEW_CUSTOMERS, MANAGE_ITEMS, VIEW_ITEM_OVERVIEW, VIEW_SHIPPING_TODAY),
    CUSTOMER(ORDER_ITEMS, VIEW_OWN_EURDERS);

    private final Set<Feature> features;

    Role(Feature... features) {
        this.features = Set.of(features);
    }

    public Set<Feature> getFeatures() {
        return features;
    }

    public boolean containsFeature(Feature feature) {
        return features.contains(feature);
    }
}
