package com.tbaar.eurder.feature.security.domain;

import com.tbaar.eurder.common.UniqueEmail;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RefreshTokenRepositoryIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-06-15T10:00:00Z");

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Autowired
    private EntityManager entityManager;

    private Credentials credentials;

    @BeforeEach
    void createCredentials() {
        credentials = credentialsRepository.saveAndFlush(new Credentials(UniqueEmail.of("jane"), "hash", Role.CUSTOMER));
    }

    @Test
    void givenAnActiveToken_whenRevokingItTwice_thenOnlyTheFirstAttemptChangesIt() {
        RefreshToken token = saved(UUID.randomUUID());

        int first = refreshTokenRepository.revokeIfActive(token.getId(), NOW);
        int second = refreshTokenRepository.revokeIfActive(token.getId(), NOW.plusSeconds(1));

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        entityManager.clear();
        assertThat(refreshTokenRepository.findById(token.getId())).hasValueSatisfying(
                reloaded -> assertThat(reloaded.getRevokedAt()).isEqualTo(NOW));
    }

    @Test
    void givenTwoFamilies_whenRevokingOne_thenTheOtherStaysActive() {
        UUID familyId = UUID.randomUUID();
        saved(familyId);
        saved(familyId);
        RefreshToken other = saved(UUID.randomUUID());

        int revoked = refreshTokenRepository.revokeFamily(familyId, NOW);

        assertThat(revoked).isEqualTo(2);
        entityManager.clear();
        assertThat(refreshTokenRepository.findById(other.getId())).hasValueSatisfying(
                reloaded -> assertThat(reloaded.getRevokedAt()).isNull());
    }

    @Test
    void givenCredentialsWithAToken_whenDeletingThem_thenTheTokenIsDeletedToo() {
        RefreshToken token = saved(UUID.randomUUID());
        entityManager.clear();

        credentialsRepository.deleteById(credentials.getId());
        credentialsRepository.flush();
        entityManager.clear();

        assertThat(refreshTokenRepository.findById(token.getId())).isEmpty();
    }

    @Test
    void givenAToken_whenCheckingAroundItsExpiry_thenExpiresExactlyAtItsLastInstant() {
        RefreshToken token = saved(UUID.randomUUID());

        assertThat(token.isExpiredAt(NOW.minusSeconds(1))).isFalse();
        assertThat(token.isExpiredAt(NOW)).isTrue();
    }

    private RefreshToken saved(UUID familyId) {
        return refreshTokenRepository.saveAndFlush(new RefreshToken(credentials, familyId, UUID.randomUUID().toString(), NOW));
    }
}
