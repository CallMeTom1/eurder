package com.tbaar.eurder.feature.security.authentication;

import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.CredentialsRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CredentialsUserDetailsService implements UserDetailsService {

    private final CredentialsRepository credentialsRepository;

    public CredentialsUserDetailsService(CredentialsRepository credentialsRepository) {
        this.credentialsRepository = credentialsRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return credentialsRepository.findByEmail(Credentials.normalizeEmail(username))
                .map(AuthenticatedUser::from)
                .orElseThrow(() -> new UsernameNotFoundException("No credentials found for " + username));
    }
}
