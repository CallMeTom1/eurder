package com.tbaar.eurder.common;

public final class ValidationErrors {

    public static final String FIELDS = "errors.field.toSet()";

    private ValidationErrors() {
    }

    public static String codesOf(String field) {
        return "errors.findAll { it.field == '%s' }.code".formatted(field);
    }

    public static String paramsOf(String field, String code) {
        return "errors.find { it.field == '%s' && it.code == '%s' }.params".formatted(field, code);
    }
}
