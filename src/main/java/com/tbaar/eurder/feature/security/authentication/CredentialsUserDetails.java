package com.tbaar.eurder.feature.security.authentication;

import com.tbaar.eurder.feature.security.domain.Credentials;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public record CredentialsUserDetails(AuthenticatedUser user, String email, String passwordHash) implements UserDetails {

    public static CredentialsUserDetails from(Credentials credentials) {
        return new CredentialsUserDetails(AuthenticatedUser.from(credentials), credentials.getEmail(), credentials.getPasswordHash());
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return user.getAuthorities();
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
