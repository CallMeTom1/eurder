package com.tbaar.eurder.feature.security.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "credentials_id")
    private Credentials credentials;

    private UUID familyId;

    private String tokenHash;

    private Instant expiresAt;

    private Instant revokedAt;

    protected RefreshToken() {
    }

    public RefreshToken(Credentials credentials, UUID familyId, String tokenHash, Instant expiresAt) {
        this.credentials = credentials;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public boolean isExpiredAt(Instant instant) {
        return !instant.isBefore(expiresAt);
    }
}
