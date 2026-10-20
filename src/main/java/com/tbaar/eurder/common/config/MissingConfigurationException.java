package com.tbaar.eurder.common.config;

import java.util.List;

public class MissingConfigurationException extends RuntimeException {

    private final List<String> missingKeys;

    public MissingConfigurationException(List<String> missingKeys) {
        super("Missing required configuration: " + String.join(", ", missingKeys));
        this.missingKeys = List.copyOf(missingKeys);
    }

    public List<String> getMissingKeys() {
        return missingKeys;
    }
}
