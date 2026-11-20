package com.tbaar.eurder.feature.security.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class AccessTokenCookieFilter extends OncePerRequestFilter {

    private final String cookieName;

    public AccessTokenCookieFilter(String cookieName) {
        this.cookieName = cookieName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Cookie cookie = WebUtils.getCookie(request, cookieName);
        if (request.getHeader(HttpHeaders.AUTHORIZATION) != null || cookie == null || cookie.getValue().isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        chain.doFilter(new BearerHeaderRequest(request, "Bearer " + cookie.getValue()), response);
    }

    private static final class BearerHeaderRequest extends HttpServletRequestWrapper {

        private final String authorization;

        private BearerHeaderRequest(HttpServletRequest request, String authorization) {
            super(request);
            this.authorization = authorization;
        }

        @Override
        public @Nullable String getHeader(String name) {
            return isAuthorization(name) ? authorization : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return isAuthorization(name) ? Collections.enumeration(List.of(authorization)) : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames());
            names.add(HttpHeaders.AUTHORIZATION);
            return Collections.enumeration(names);
        }

        private static boolean isAuthorization(String name) {
            return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name);
        }
    }
}
