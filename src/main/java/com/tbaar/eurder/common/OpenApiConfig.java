package com.tbaar.eurder.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Eurder", version = "1.0", description = "Order management back-end"))
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
public class OpenApiConfig {

    private static final String PROBLEM_DETAIL = "ProblemDetail";

    @Bean
    public OpenApiCustomizer problemDetailSchema() {
        return openApi -> openApi.getComponents().addSchemas(PROBLEM_DETAIL, new ObjectSchema()
                .description("Error response (RFC 9457)")
                .addProperty("type", new StringSchema().example("about:blank"))
                .addProperty("title", new StringSchema().example("Bad Request"))
                .addProperty("status", new IntegerSchema().example(400))
                .addProperty("detail", new StringSchema().example("Validation failed"))
                .addProperty("instance", new StringSchema().example("/items"))
                .addProperty("errors", new MapSchema()
                        .additionalProperties(new ArraySchema().items(new StringSchema()))
                        .description("Only for validation errors: the messages of each invalid field, sorted")
                        .example(Map.of("price", List.of("Price must be greater than 0")))));
    }

    @Bean
    public OperationCustomizer standardErrorResponses() {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            if (hasParameterAnnotatedWith(handlerMethod, RequestBody.class, PathVariable.class, RequestParam.class)) {
                addIfAbsent(responses, "400", "Invalid request: a field, a parameter or the JSON body is invalid");
            }
            if (handlerMethod.hasMethodAnnotation(Secured.class)) {
                addIfAbsent(responses, "401", "Missing or invalid credentials");
                addIfAbsent(responses, "403", "The role of the authenticated user lacks the required feature");
            }
            if (hasParameterAnnotatedWith(handlerMethod, PathVariable.class)) {
                addIfAbsent(responses, "404", "No resource found for the given id");
            }
            responses.forEach((code, response) -> {
                if (code.startsWith("4")) {
                    response.content(problemContent());
                }
            });
            return operation;
        };
    }

    @SafeVarargs
    private static boolean hasParameterAnnotatedWith(HandlerMethod handlerMethod, Class<? extends Annotation>... annotations) {
        return Arrays.stream(handlerMethod.getMethodParameters())
                .anyMatch(parameter -> Arrays.stream(annotations).anyMatch(parameter::hasParameterAnnotation));
    }

    private static void addIfAbsent(ApiResponses responses, String code, String description) {
        if (!responses.containsKey(code)) {
            responses.addApiResponse(code, new ApiResponse().description(description));
        }
    }

    private static Content problemContent() {
        return new Content().addMediaType("application/problem+json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM_DETAIL)));
    }
}
