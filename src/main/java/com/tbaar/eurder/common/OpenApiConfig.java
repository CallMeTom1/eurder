package com.tbaar.eurder.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Eurder", version = "1.0", description = "Order management back-end"))
public class OpenApiConfig {}
