package com.tbaar.eurder.feature.item.api.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CreateItemDtoValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void givenCompleteItem_whenValidating_thenHasNoViolation() {
        CreateItemDto dto = validItem();

        Set<ConstraintViolation<CreateItemDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void givenItemOutOfStock_whenValidating_thenHasNoViolation() {
        CreateItemDto dto = withStock(0);

        Set<ConstraintViolation<CreateItemDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest(name = "price {0} is accepted")
    @ValueSource(strings = {"0.01", "10", "9.9", "99999999.99"})
    void givenPositivePriceWithAtMostTwoDecimals_whenValidating_thenHasNoViolation(String price) {
        CreateItemDto dto = withPrice(new BigDecimal(price));

        Set<ConstraintViolation<CreateItemDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest(name = "{0} = \"{1}\" is rejected")
    @MethodSource("invalidFields")
    void givenOneInvalidField_whenValidating_thenReportsThatFieldWithItsMessage(String field, CreateItemDto dto, String expectedMessage) {
        Map<String, Set<String>> messagesByField = messagesByField(validator.validate(dto));

        assertThat(messagesByField).containsOnlyKeys(field);
        assertThat(messagesByField.get(field)).contains(expectedMessage);
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                Arguments.of("name", withName(null), "Name is required"),
                Arguments.of("name", withName(" "), "Name is required"),
                Arguments.of("name", withName("a".repeat(256)), "Name must be at most 255 characters"),
                Arguments.of("description", withDescription(""), "Description is required"),
                Arguments.of("description", withDescription("a".repeat(1001)), "Description must be at most 1000 characters"),
                Arguments.of("price", withPrice(null), "Price is required"),
                Arguments.of("price", withPrice(BigDecimal.ZERO), "Price must be greater than 0"),
                Arguments.of("price", withPrice(new BigDecimal("-1.50")), "Price must be greater than 0"),
                Arguments.of("price", withPrice(new BigDecimal("9.999")), "Price must have at most 8 digits before and 2 after the decimal point"),
                Arguments.of("price", withPrice(new BigDecimal("100000000")), "Price must have at most 8 digits before and 2 after the decimal point"),
                Arguments.of("stock", withStock(null), "Stock is required"),
                Arguments.of("stock", withStock(-1), "Stock must be 0 or more")
        );
    }

    private static Map<String, Set<String>> messagesByField(Set<ConstraintViolation<CreateItemDto>> violations) {
        return violations.stream().collect(Collectors.groupingBy(
                violation -> violation.getPropertyPath().toString(),
                Collectors.mapping(ConstraintViolation::getMessage, Collectors.toSet())));
    }

    private static CreateItemDto validItem() {
        return new CreateItemDto("Chair", "Wooden chair", new BigDecimal("49.99"), 10);
    }

    private static CreateItemDto withName(String name) {
        return new CreateItemDto(name, "Wooden chair", new BigDecimal("49.99"), 10);
    }

    private static CreateItemDto withDescription(String description) {
        return new CreateItemDto("Chair", description, new BigDecimal("49.99"), 10);
    }

    private static CreateItemDto withPrice(BigDecimal price) {
        return new CreateItemDto("Chair", "Wooden chair", price, 10);
    }

    private static CreateItemDto withStock(Integer stock) {
        return new CreateItemDto("Chair", "Wooden chair", new BigDecimal("49.99"), stock);
    }
}
