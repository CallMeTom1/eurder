package com.tbaar.eurder.common.openapi;

import com.tbaar.eurder.common.exception.ErrorCode;
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
import java.util.Map;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Eurder", version = "1.0", description = """
        Order management back-end.

        Log in with POST /auth/login, then paste the access token in the Authorize button: \
        each operation states the feature it requires and the roles having it.

        Every error is a ProblemDetail (RFC 9457) with a translatable `code` and the `params` to insert in its translation; \
        validation errors also list the code of each invalid field.
        """))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
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
                .addProperty("code", errorCodeSchema("Translatable key of the error").example(ErrorCode.VALIDATION_FAILED.key()))
                .addProperty("params", paramsSchema())
                .addProperty("errors", new ArraySchema()
                        .description("Only for validation errors: one entry per broken constraint, sorted by field")
                        .items(new ObjectSchema()
                                .addProperty("field", new StringSchema().example("name"))
                                .addProperty("code", errorCodeSchema("Translatable key of the broken constraint")
                                        .example(ErrorCode.VALIDATION_TOO_LONG.key()))
                                .addProperty("params", paramsSchema()))));
    }

    private static Schema<String> errorCodeSchema(String description) {
        StringSchema schema = new StringSchema();
        schema.setEnum(Arrays.stream(ErrorCode.values()).map(ErrorCode::key).toList());
        return schema.description(description);
    }

    private static Schema<Object> paramsSchema() {
        return new MapSchema()
                .description("Values to insert in the translation, such as an id or a maximum length")
                .example(Map.of("max", 255));
    }

    @Bean
    public OperationCustomizer standardErrorResponses() {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            if (hasParameterAnnotatedWith(handlerMethod, RequestBody.class, PathVariable.class, RequestParam.class)) {
                addIfAbsent(responses, "400", "Invalid request: a field, a parameter or the JSON body is invalid");
            }
            if (handlerMethod.hasMethodAnnotation(Secured.class)) {
                addIfAbsent(responses, "401", "Missing, invalid or expired access token");
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
