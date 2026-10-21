package com.tbaar.eurder.feature.security.authentication;

import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.CredentialsRepository;
import com.tbaar.eurder.feature.security.domain.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialsUserDetailsServiceTest {

    @Mock
    private CredentialsRepository credentialsRepository;

    @InjectMocks
    private CredentialsUserDetailsService userDetailsService;

    @Test
    void givenAnEmailWithSpacesAndCapitals_whenLoadingTheUser_thenLooksUpTheNormalizedEmail() {
        when(credentialsRepository.findByEmail("jane@doe.be")).thenReturn(Optional.of(new Credentials("jane@doe.be", "hash", Role.CUSTOMER)));

        UserDetails user = userDetailsService.loadUserByUsername(" Jane@Doe.BE ");

        assertThat(user.getUsername()).isEqualTo("jane@doe.be");
    }

    @Test
    void givenAnUnknownEmail_whenLoadingTheUser_thenThrowsUsernameNotFound() {
        when(credentialsRepository.findByEmail("nobody@eurder.be")).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> userDetailsService.loadUserByUsername("nobody@eurder.be"));

        assertThat(thrown).isInstanceOf(UsernameNotFoundException.class);
    }
}
