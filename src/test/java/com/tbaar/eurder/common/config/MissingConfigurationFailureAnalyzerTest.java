package com.tbaar.eurder.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MissingConfigurationFailureAnalyzerTest {

    @Test
    void givenMissingKeys_whenAnalyzingTheStartupFailure_thenDescribesThemAndHowToProvideThem() {
        MissingConfigurationException failure = new MissingConfigurationException(List.of("DB_URL", "DB_PASSWORD"));

        FailureAnalysis analysis = new MissingConfigurationFailureAnalyzer().analyze(failure);

        assertThat(analysis.getDescription()).isEqualTo("Missing required configuration: DB_URL, DB_PASSWORD");
        assertThat(analysis.getAction())
                .contains("Copy .env.example to .env")
                .contains("DB_URL, DB_PASSWORD")
                .contains("environment variables");
    }
}
