package com.tbaar.eurder.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class RequiredConfigurationEnvironmentPostProcessorTest {

    private final RequiredConfigurationEnvironmentPostProcessor postProcessor = new RequiredConfigurationEnvironmentPostProcessor();

    @Test
    void givenEveryRequiredKey_whenTheApplicationStarts_thenAcceptsTheConfiguration() {
        MockEnvironment environment = completeEnvironment();

        Throwable thrown = catchThrowable(() -> postProcessor.postProcessEnvironment(environment, new SpringApplication()));

        assertThat(thrown).isNull();
    }

    @Test
    void givenAMissingKey_whenTheApplicationStarts_thenRefusesNamingThatKey() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("DB_URL", "jdbc:postgresql://localhost:5432/eurder")
                .withProperty("DB_USERNAME", "eurder");

        MissingConfigurationException thrown = catchThrowableOfType(MissingConfigurationException.class,
                () -> postProcessor.postProcessEnvironment(environment, new SpringApplication()));

        assertThat(thrown).hasMessage("Missing required configuration: DB_PASSWORD");
    }

    @Test
    void givenSeveralMissingKeys_whenTheApplicationStarts_thenListsThemAllAtOnce() {
        MockEnvironment environment = new MockEnvironment().withProperty("DB_USERNAME", "eurder");

        MissingConfigurationException thrown = catchThrowableOfType(MissingConfigurationException.class,
                () -> postProcessor.postProcessEnvironment(environment, new SpringApplication()));

        assertThat(thrown.getMissingKeys()).containsExactly("DB_URL", "DB_PASSWORD");
    }

    @ParameterizedTest(name = "DB_PASSWORD = \"{0}\" counts as missing")
    @ValueSource(strings = {"", "   "})
    void givenABlankKey_whenTheApplicationStarts_thenTreatsItAsMissingWithoutShowingItsValue(String blankPassword) {
        MockEnvironment environment = completeEnvironment().withProperty("DB_PASSWORD", blankPassword);

        MissingConfigurationException thrown = catchThrowableOfType(MissingConfigurationException.class,
                () -> postProcessor.postProcessEnvironment(environment, new SpringApplication()));

        assertThat(thrown).hasMessage("Missing required configuration: DB_PASSWORD");
    }

    private static MockEnvironment completeEnvironment() {
        return new MockEnvironment()
                .withProperty("DB_URL", "jdbc:postgresql://localhost:5432/eurder")
                .withProperty("DB_USERNAME", "eurder")
                .withProperty("DB_PASSWORD", "s3cret");
    }
}
