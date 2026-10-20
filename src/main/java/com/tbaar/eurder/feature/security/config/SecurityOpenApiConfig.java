package com.tbaar.eurder.feature.security.config;

import com.tbaar.eurder.feature.security.domain.Feature;
import com.tbaar.eurder.feature.security.domain.Role;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.annotation.Secured;

import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
public class SecurityOpenApiConfig {

    @Bean
    public OperationCustomizer requiredFeatureDescription() {
        return (operation, handlerMethod) -> {
            Secured secured = handlerMethod.getMethodAnnotation(Secured.class);
            String access = secured == null
                    ? "Public: no authentication needed."
                    : Arrays.stream(secured.value()).map(SecurityOpenApiConfig::requirement).collect(Collectors.joining(" "));
            operation.setDescription(operation.getDescription() == null ? access : operation.getDescription() + "\n\n" + access);
            return operation;
        };
    }

    private static String requirement(String feature) {
        String roles = Arrays.stream(Role.values())
                .filter(role -> role.getFeatures().contains(Feature.valueOf(feature)))
                .map(Role::name)
                .collect(Collectors.joining(", "));
        return "Requires the " + feature + " feature (role " + roles + ").";
    }
}
