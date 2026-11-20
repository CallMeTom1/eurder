package com.tbaar.eurder.feature.security.service;

import com.tbaar.eurder.feature.security.api.dto.CurrentUserDto;

public record AuthenticationResult(String accessToken, String refreshToken, CurrentUserDto user) {
}
