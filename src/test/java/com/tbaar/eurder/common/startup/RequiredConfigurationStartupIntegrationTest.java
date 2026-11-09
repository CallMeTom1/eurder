package com.tbaar.eurder.common.startup;

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
    void givenABlankUri_whenStartingTheApplication_thenItStopsAndLogsWhichKeyIsMissing(CapturedOutput output) {
        SpringApplication application = new SpringApplication(EurderApplication.class);

        Throwable thrown = catchThrowable(() -> application.run("--MONGODB_URI="));

        assertThat(thrown).isInstanceOf(MissingConfigurationException.class);
        assertThat(output)
                .contains("APPLICATION FAILED TO START")
                .contains("Missing required configuration: MONGODB_URI")
                .contains("Copy .env.example to .env");
    }
}
