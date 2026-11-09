package com.tbaar.eurder.feature.security.authentication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class AccessTokenIssuer {

    public static final String ISSUER = "eurder";
    public static final String ROLE_CLAIM = "role";

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final Duration timeToLive;

    public AccessTokenIssuer(JwtEncoder jwtEncoder, Clock clock, @Value("${eurder.jwt.access-token-ttl}") Duration timeToLive) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.timeToLive = timeToLive;
    }

    public String issue(AuthenticatedUser user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.getName())
                .claim(ROLE_CLAIM, user.role().name())
                .issuedAt(now)
                .expiresAt(now.plus(timeToLive))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long timeToLiveInSeconds() {
        return timeToLive.toSeconds();
    }
}
