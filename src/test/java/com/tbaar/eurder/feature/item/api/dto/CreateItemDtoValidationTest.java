package com.tbaar.eurder.feature.item.api.dto;

import com.tbaar.eurder.feature.item.domain.ItemState;
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

import java.math.BigDecimal;
import java.util.HashMap;
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
    void givenUnitsInSeveralStates_whenValidating_thenHasNoViolation() {
        CreateItemDto dto = withUnits(Map.of(ItemState.PRISTINE, 1000, ItemState.DAMAGED, 0));

        Set<ConstraintViolation<CreateItemDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void givenNoUnitAtAll_whenValidating_thenHasNoViolation() {
        CreateItemDto dto = withUnits(Map.of());

        Set<ConstraintViolation<CreateItemDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest(name = "{0} is rejected with \"{2}\"")
    @MethodSource("invalidFields")
    void givenOneInvalidField_whenValidating_thenReportsThatFieldWithItsMessage(String field, CreateItemDto dto, String expectedMessage) {
        Map<String, Set<String>> messagesByField = messagesByField(validator.validate(dto));

        assertThat(messagesByField).containsOnlyKeys(field);
        assertThat(messagesByField.get(field)).contains(expectedMessage);
    }

    static Stream<Arguments> invalidFields() {
        Map<ItemState, Integer> missingCount = new HashMap<>();
        missingCount.put(ItemState.PRISTINE, null);
        return Stream.of(
                Arguments.of("name", new CreateItemDto(" ", "Wooden chair", new BigDecimal("49.99"), Map.of()), "Name is required"),
                Arguments.of("description", new CreateItemDto("Chair", "a".repeat(1001), new BigDecimal("49.99"), Map.of()), "Description must be at most 1000 characters"),
                Arguments.of("price", new CreateItemDto("Chair", "Wooden chair", new BigDecimal("9.999"), Map.of()), "Price must have at most 8 digits before and 2 after the decimal point"),
                Arguments.of("units", withUnits(null), "Units are required"),
                Arguments.of("units[PRISTINE].<map value>", withUnits(missingCount), "Units per state are required"),
                Arguments.of("units[DAMAGED].<map value>", withUnits(Map.of(ItemState.DAMAGED, -1)), "Units per state must be 0 or more"),
                Arguments.of("units[PRISTINE].<map value>", withUnits(Map.of(ItemState.PRISTINE, 1001)), "Units per state must be at most 1000")
        );
    }

    private static Map<String, Set<String>> messagesByField(Set<ConstraintViolation<CreateItemDto>> violations) {
        return violations.stream().collect(Collectors.groupingBy(
                violation -> violation.getPropertyPath().toString(),
                Collectors.mapping(ConstraintViolation::getMessage, Collectors.toSet())));
    }

    private static CreateItemDto withUnits(Map<ItemState, Integer> units) {
        return new CreateItemDto("Chair", "Wooden chair", new BigDecimal("49.99"), units);
    }
}
