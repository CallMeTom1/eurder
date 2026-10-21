package com.tbaar.eurder.common.openapi;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Component
public class EnumKeyMapModelConverter implements ModelConverter {

    @Override
    public Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        Schema<?> schema = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        JavaType javaType = Json.mapper().constructType(type.getType());
        if (schema != null && javaType.isMapLikeType() && javaType.getKeyType().isEnumType()) {
            List<String> keys = Arrays.stream(javaType.getKeyType().getRawClass().getEnumConstants())
                    .map(key -> ((Enum<?>) key).name())
                    .toList();
            schema.propertyNames(new StringSchema()._enum(keys));
        }
        return schema;
    }
}
