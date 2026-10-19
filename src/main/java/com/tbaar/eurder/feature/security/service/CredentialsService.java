package com.tbaar.eurder.feature.security.service;

import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.CredentialsRepository;
import com.tbaar.eurder.feature.security.domain.Role;
import com.tbaar.eurder.feature.security.exception.EmailAlreadyUsedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class CredentialsService {

    private final CredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;

    public CredentialsService(CredentialsRepository credentialsRepository, PasswordEncoder passwordEncoder) {
        this.credentialsRepository = credentialsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Credentials create(String email, String rawPassword, Role role) {
        String normalizedEmail = normalize(email);
        if (credentialsRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyUsedException(normalizedEmail);
        }
        String passwordHash = passwordEncoder.encode(rawPassword);
        try {
            return credentialsRepository.saveAndFlush(new Credentials(normalizedEmail, passwordHash, role));
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyUsedException(normalizedEmail, e);
        }
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
