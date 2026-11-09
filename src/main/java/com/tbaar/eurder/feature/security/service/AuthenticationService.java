package com.tbaar.eurder.feature.security.service;

import com.tbaar.eurder.feature.security.api.dto.LoginDto;
import com.tbaar.eurder.feature.security.api.dto.TokensDto;
import com.tbaar.eurder.feature.security.authentication.AccessTokenIssuer;
import com.tbaar.eurder.feature.security.authentication.AuthenticatedUser;
import com.tbaar.eurder.feature.security.authentication.CredentialsUserDetails;
import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.CredentialsRepository;
import com.tbaar.eurder.feature.security.domain.RefreshToken;
import com.tbaar.eurder.feature.security.domain.RefreshTokenRepository;
import com.tbaar.eurder.feature.security.exception.InvalidRefreshTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthenticationService {

    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final AccessTokenIssuer accessTokenIssuer;
    private final CredentialsRepository credentialsRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;
    private final Duration refreshTokenTimeToLive;

    public AuthenticationService(AuthenticationManager authenticationManager, AccessTokenIssuer accessTokenIssuer,
                                 CredentialsRepository credentialsRepository, RefreshTokenRepository refreshTokenRepository,
                                 Clock clock, @Value("${eurder.jwt.refresh-token-ttl}") Duration refreshTokenTimeToLive) {
        this.authenticationManager = authenticationManager;
        this.accessTokenIssuer = accessTokenIssuer;
        this.credentialsRepository = credentialsRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
        this.refreshTokenTimeToLive = refreshTokenTimeToLive;
    }

    public TokensDto login(LoginDto request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        AuthenticatedUser user = ((CredentialsUserDetails) authentication.getPrincipal()).user();
        return issueTokens(user, credentialsRepository.getReferenceById(user.id()), UUID.randomUUID());
    }

    // The revocation of a reused token must survive the exception that refuses the request.
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public TokensDto refresh(String rawToken) {
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash(rawToken)).orElseThrow(InvalidRefreshTokenException::new);
        Instant now = clock.instant();
        if (current.isExpiredAt(now)) {
            throw new InvalidRefreshTokenException();
        }
        if (refreshTokenRepository.revokeIfActive(current.getId(), now) == 0) {
            refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }
        Credentials credentials = current.getCredentials();
        return issueTokens(AuthenticatedUser.from(credentials), credentials, current.getFamilyId());
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> refreshTokenRepository.revokeFamily(token.getFamilyId(), clock.instant()));
    }

    private TokensDto issueTokens(AuthenticatedUser user, Credentials credentials, UUID familyId) {
        String rawRefreshToken = newRefreshToken();
        refreshTokenRepository.save(new RefreshToken(credentials, familyId, hash(rawRefreshToken), clock.instant().plus(refreshTokenTimeToLive)));
        return new TokensDto(accessTokenIssuer.issue(user), rawRefreshToken, "Bearer", accessTokenIssuer.timeToLiveInSeconds());
    }

    private static String newRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
