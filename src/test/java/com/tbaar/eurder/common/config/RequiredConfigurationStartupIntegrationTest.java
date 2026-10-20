package com.tbaar.eurder.common.config;

import com.tbaar.eurder.EurderApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ExtendWith(OutputCaptureExtension.class)
class RequiredConfigurationStartupIntegrationTest {

    @Test
    void givenABlankPassword_whenStartingTheApplication_thenItStopsAndLogsWhichKeyIsMissing(CapturedOutput output) {
        SpringApplication application = new SpringApplication(EurderApplication.class);

        Throwable thrown = catchThrowable(() -> application.run("--DB_PASSWORD="));

        assertThat(thrown).isInstanceOf(MissingConfigurationException.class);
        assertThat(output)
                .contains("APPLICATION FAILED TO START")
                .contains("Missing required configuration: DB_PASSWORD")
                .contains("Copy .env.example to .env");
    }
}
