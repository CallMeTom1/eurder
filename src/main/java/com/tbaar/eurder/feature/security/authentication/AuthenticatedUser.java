package com.tbaar.eurder.feature.security.authentication;

import com.tbaar.eurder.feature.security.domain.Credentials;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(UUID id,
                                String email,
                                String passwordHash,
                                List<GrantedAuthority> authorities) implements UserDetails {

    public static AuthenticatedUser from(Credentials credentials) {
        List<GrantedAuthority> authorities = credentials.getRole().getFeatures().stream()
                .<GrantedAuthority>map(feature -> new SimpleGrantedAuthority(feature.name()))
                .toList();
        return new AuthenticatedUser(credentials.getId(), credentials.getEmail(), credentials.getPasswordHash(), authorities);
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
