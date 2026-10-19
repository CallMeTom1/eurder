package com.tbaar.eurder.feature.security.service;

import com.tbaar.eurder.common.exception.UnauthorizedException;
import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.CredentialsRepository;
import com.tbaar.eurder.feature.security.domain.Feature;
import com.tbaar.eurder.feature.security.exception.FeatureAccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Transactional(readOnly = true)
public class SecurityService {

    private static final String BASIC_PREFIX = "Basic ";

    private final CredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;

    public SecurityService(CredentialsRepository credentialsRepository, PasswordEncoder passwordEncoder) {
        this.credentialsRepository = credentialsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Credentials validateAuthorization(String authorization, Feature feature) {
        Credentials credentials = authenticate(authorization);
        if (!credentials.canHaveAccessTo(feature)) {
            throw new FeatureAccessDeniedException(credentials.getEmail(), feature);
        }
        return credentials;
    }

    private Credentials authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith(BASIC_PREFIX)) {
            throw new UnauthorizedException();
        }
        String decoded = decode(authorization.substring(BASIC_PREFIX.length()));
        String[] emailAndPassword = decoded.split(":", 2);
        if (emailAndPassword.length != 2) {
            throw new UnauthorizedException();
        }
        String email = Credentials.normalizeEmail(emailAndPassword[0]);
        String password = emailAndPassword[1];
        return credentialsRepository.findByEmail(email)
                .filter(found -> passwordEncoder.matches(password, found.getPasswordHash()))
                .orElseThrow(UnauthorizedException::new);
    }

    private String decode(String encodedCredentials) {
        try {
            return new String(Base64.getDecoder().decode(encodedCredentials), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException();
        }
    }
}
