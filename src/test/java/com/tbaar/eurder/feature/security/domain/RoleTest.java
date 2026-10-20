package com.tbaar.eurder.feature.security.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @ParameterizedTest(name = "{0} has access to {1}: {2}")
    @CsvSource({
            "ADMIN,    VIEW_CUSTOMERS,      true",
            "ADMIN,    MANAGE_ITEMS,        true",
            "ADMIN,    VIEW_ITEM_OVERVIEW,  true",
            "ADMIN,    VIEW_SHIPPING_TODAY, true",
            "ADMIN,    ORDER_ITEMS,         false",
            "ADMIN,    VIEW_OWN_EURDERS,    false",
            "CUSTOMER, VIEW_CUSTOMERS,      false",
            "CUSTOMER, MANAGE_ITEMS,        false",
            "CUSTOMER, VIEW_ITEM_OVERVIEW,  false",
            "CUSTOMER, VIEW_SHIPPING_TODAY, false",
            "CUSTOMER, ORDER_ITEMS,         true",
            "CUSTOMER, VIEW_OWN_EURDERS,    true"
    })
    void givenRoleAndFeature_whenCheckingAccess_thenMatchesTheRightsMatrix(Role role, Feature feature, boolean expectedAccess) {
        boolean hasAccess = role.getFeatures().contains(feature);

        assertThat(hasAccess).isEqualTo(expectedAccess);
    }

    @ParameterizedTest(name = "{0} is granted to at least one role")
    @EnumSource(Feature.class)
    void givenAnyFeature_whenCheckingEveryRole_thenAtLeastOneRoleHasIt(Feature feature) {
        Role[] allRoles = Role.values();

        boolean grantedToSomeone = Arrays.stream(allRoles).anyMatch(role -> role.getFeatures().contains(feature));

        assertThat(grantedToSomeone).isTrue();
    }
}
