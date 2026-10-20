package com.tbaar.eurder.common.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.List;

public class RequiredConfigurationEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final List<String> REQUIRED_KEYS = List.of("DB_URL", "DB_USERNAME", "DB_PASSWORD");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        List<String> missingKeys = REQUIRED_KEYS.stream()
                .filter(key -> isBlank(environment.getProperty(key)))
                .toList();
        if (!missingKeys.isEmpty()) {
            throw new MissingConfigurationException(missingKeys);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
