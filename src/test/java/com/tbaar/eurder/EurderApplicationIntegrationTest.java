package com.tbaar.eurder;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EurderApplicationIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void givenTestConfiguration_whenApplicationStarts_thenContextIsLoaded() {
        String applicationName = "eurder";

        String loadedName = context.getEnvironment().getProperty("spring.application.name");

        assertThat(loadedName).isEqualTo(applicationName);
    }
}
