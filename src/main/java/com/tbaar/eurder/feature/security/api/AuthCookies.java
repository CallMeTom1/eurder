package com.tbaar.eurder.feature.security.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookies {

    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";

    private final String accessTokenPath;
    private final String refreshTokenPath;
    private final Duration timeToLive;

    public AuthCookies(@Value("${server.servlet.context-path:}") String contextPath,
                       @Value("${eurder.jwt.refresh-token-ttl}") Duration timeToLive) {
        this.accessTokenPath = contextPath.isEmpty() ? "/" : contextPath;
        this.refreshTokenPath = contextPath + "/auth";
        this.timeToLive = timeToLive;
    }

    public HttpHeaders issue(String accessToken, String refreshToken) {
        return headers(
                cookie(ACCESS_TOKEN, accessToken, accessTokenPath, timeToLive),
                cookie(REFRESH_TOKEN, refreshToken, refreshTokenPath, timeToLive));
    }

    public HttpHeaders clear() {
        return headers(
                cookie(ACCESS_TOKEN, "", accessTokenPath, Duration.ZERO),
                cookie(REFRESH_TOKEN, "", refreshTokenPath, Duration.ZERO));
    }

    private static ResponseCookie cookie(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(path)
                .maxAge(maxAge)
                .build();
    }

    private static HttpHeaders headers(ResponseCookie accessToken, ResponseCookie refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, accessToken.toString());
        headers.add(HttpHeaders.SET_COOKIE, refreshToken.toString());
        return headers;
    }
}
