package com.tbaar.eurder.feature.security.authentication;

import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(UUID id, Role role) implements Principal {

    public static AuthenticatedUser from(Credentials credentials) {
        return new AuthenticatedUser(credentials.getId(), credentials.getRole());
    }

    public List<GrantedAuthority> getAuthorities() {
        return role.getFeatures().stream()
                .<GrantedAuthority>map(feature -> new SimpleGrantedAuthority(feature.name()))
                .toList();
    }

    @Override
    public String getName() {
        return id.toString();
    }
}
