package com.tbaar.eurder.feature.security.config;

import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.CredentialsRepository;
import com.tbaar.eurder.feature.security.domain.Role;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DefaultAdminInitializer implements ApplicationRunner {

    private static final String EMAIL = "admin@eurder.be";
    private static final String PASSWORD_HASH = "$2a$10$hnwthCbYOsR2LMxuXUiW5OmU0PIVFFxex.yCB4kb/IMqbLIRJmJm2";

    private final CredentialsRepository credentialsRepository;

    public DefaultAdminInitializer(CredentialsRepository credentialsRepository) {
        this.credentialsRepository = credentialsRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!credentialsRepository.existsByEmail(EMAIL)) {
            credentialsRepository.save(new Credentials(EMAIL, PASSWORD_HASH, Role.ADMIN));
        }
    }
}
