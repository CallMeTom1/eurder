package com.tbaar.eurder.feature.security.authentication;

import com.tbaar.eurder.feature.security.domain.Role;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AccessTokenAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        AuthenticatedUser user = userOf(jwt);
        return UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
    }

    private static AuthenticatedUser userOf(Jwt jwt) {
        try {
            return new AuthenticatedUser(UUID.fromString(jwt.getSubject()), Role.valueOf(jwt.getClaimAsString(AccessTokenIssuer.ROLE_CLAIM)));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidBearerTokenException("The access token has no valid subject or role", e);
        }
    }
}
