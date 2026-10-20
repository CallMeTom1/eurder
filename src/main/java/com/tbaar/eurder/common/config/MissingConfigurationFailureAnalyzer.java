package com.tbaar.eurder.common.config;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class MissingConfigurationFailureAnalyzer extends AbstractFailureAnalyzer<MissingConfigurationException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, MissingConfigurationException cause) {
        String keys = String.join(", ", cause.getMissingKeys());
        return new FailureAnalysis(cause.getMessage(),
                "Copy .env.example to .env and fill in the missing keys (" + keys + "), or set them as environment variables. "
                        + "The .env file uses the .properties format: write the values without quotes.",
                cause);
    }
}
