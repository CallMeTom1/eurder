package com.tbaar.eurder.feature.security.service;

import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticatedUserTest {

    @Test
    void givenCredentialsOfAnAdmin_whenAuthenticatingThem_thenTheAuthoritiesAreTheFeaturesOfTheAdminRole() {
        Credentials credentials = new Credentials("admin@eurder.be", "hash", Role.ADMIN);

        AuthenticatedUser user = AuthenticatedUser.from(credentials);

        assertThat(user.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("VIEW_CUSTOMERS", "MANAGE_ITEMS", "VIEW_ITEM_OVERVIEW", "VIEW_SHIPPING_TODAY");
    }

    @Test
    void givenCredentialsOfACustomer_whenAuthenticatingThem_thenUsesTheEmailAndHashAsUsernameAndPassword() {
        Credentials credentials = new Credentials("jane@doe.be", "hash", Role.CUSTOMER);

        AuthenticatedUser user = AuthenticatedUser.from(credentials);

        assertThat(user.getUsername()).isEqualTo("jane@doe.be");
        assertThat(user.getPassword()).isEqualTo("hash");
        assertThat(user.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ORDER_ITEMS", "VIEW_OWN_EURDERS");
    }
}
