package com.tbaar.eurder.feature.security.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordEncoder {

    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    public String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        return bCryptPasswordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String hash) {
        return bCryptPasswordEncoder.matches(rawPassword, hash);
    }
}
