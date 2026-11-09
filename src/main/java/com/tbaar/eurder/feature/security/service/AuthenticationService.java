package com.tbaar.eurder.feature.security.service;

import com.tbaar.eurder.feature.security.api.dto.LoginDto;
import com.tbaar.eurder.feature.security.api.dto.TokensDto;
import com.tbaar.eurder.feature.security.authentication.AccessTokenIssuer;
import com.tbaar.eurder.feature.security.authentication.AuthenticatedUser;
import com.tbaar.eurder.feature.security.authentication.CredentialsUserDetails;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final AccessTokenIssuer accessTokenIssuer;

    public AuthenticationService(AuthenticationManager authenticationManager, AccessTokenIssuer accessTokenIssuer) {
        this.authenticationManager = authenticationManager;
        this.accessTokenIssuer = accessTokenIssuer;
    }

    public TokensDto login(LoginDto request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        AuthenticatedUser user = ((CredentialsUserDetails) authentication.getPrincipal()).user();
        return new TokensDto(accessTokenIssuer.issue(user), "Bearer", accessTokenIssuer.timeToLiveInSeconds());
    }
}
