package com.tbaar.eurder.common.startup;

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
        MockEnvironment environment = new MockEnvironment().withProperty("MONGODB_URI", "mongodb://127.0.0.1:27017/eurder");

        Throwable thrown = catchThrowable(() -> postProcessor.postProcessEnvironment(environment, new SpringApplication()));

        assertThat(thrown).isNull();
    }

    @Test
    void givenAMissingKey_whenTheApplicationStarts_thenRefusesNamingThatKey() {
        MockEnvironment environment = new MockEnvironment();

        MissingConfigurationException thrown = catchThrowableOfType(MissingConfigurationException.class,
                () -> postProcessor.postProcessEnvironment(environment, new SpringApplication()));

        assertThat(thrown).hasMessage("Missing required configuration: MONGODB_URI");
    }

    @ParameterizedTest(name = "MONGODB_URI = \"{0}\" counts as missing")
    @ValueSource(strings = {"", "   "})
    void givenABlankKey_whenTheApplicationStarts_thenTreatsItAsMissingWithoutShowingItsValue(String blankUri) {
        MockEnvironment environment = new MockEnvironment().withProperty("MONGODB_URI", blankUri);

        MissingConfigurationException thrown = catchThrowableOfType(MissingConfigurationException.class,
                () -> postProcessor.postProcessEnvironment(environment, new SpringApplication()));

        assertThat(thrown).hasMessage("Missing required configuration: MONGODB_URI");
    }
}
