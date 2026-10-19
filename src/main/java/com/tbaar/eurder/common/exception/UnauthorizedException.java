package com.tbaar.eurder.common.exception;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException() {
        super("Invalid credentials");
    }
}
