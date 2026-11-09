package com.tbaar.eurder.feature.security.authentication;

import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticatedUserTest {

    @Test
    void givenCredentialsOfAnAdmin_whenAuthenticatingThem_thenTheAuthoritiesAreTheFeaturesOfTheAdminRole() {
        Credentials credentials = new Credentials("admin@eurder.be", "hash", Role.ADMIN);

        AuthenticatedUser user = AuthenticatedUser.from(credentials);

        assertThat(user.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("VIEW_CUSTOMERS", "MANAGE_ITEMS", "VIEW_ITEM_OVERVIEW",
                        "VIEW_SHIPPING_TODAY", "REGISTER_ADMIN");
    }

    @Test
    void givenCredentialsOfACustomer_whenLoadingThemForALogin_thenUsesTheEmailAndHashAsUsernameAndPassword() {
        Credentials credentials = new Credentials("jane@doe.be", "hash", Role.CUSTOMER);

        CredentialsUserDetails details = CredentialsUserDetails.from(credentials);

        assertThat(details.getUsername()).isEqualTo("jane@doe.be");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ORDER_ITEMS", "VIEW_OWN_EURDERS");
    }

    @Test
    void givenAUser_whenAskingItsName_thenReturnsItsIdSinceTokensCarryNoEmail() {
        UUID id = UUID.randomUUID();

        String name = new AuthenticatedUser(id, Role.CUSTOMER).getName();

        assertThat(name).isEqualTo(id.toString());
    }
}
