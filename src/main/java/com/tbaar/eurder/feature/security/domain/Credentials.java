package com.tbaar.eurder.feature.security.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Locale;
import java.util.UUID;

@Document(collection = "credentials")
public class Credentials {

    @Id
    private UUID id = UUID.randomUUID();

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private Role role;

    protected Credentials() {
    }

    public Credentials(String email, String passwordHash, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
