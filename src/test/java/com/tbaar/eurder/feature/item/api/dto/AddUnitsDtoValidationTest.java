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

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AddUnitsDtoValidationTest {

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
    void givenStateAndAmountWithinLimits_whenValidating_thenHasNoViolation() {
        AddUnitsDto dto = new AddUnitsDto(ItemState.DAMAGED, 1000);

        Set<ConstraintViolation<AddUnitsDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest(name = "{0} is rejected with \"{2}\"")
    @MethodSource("invalidFields")
    void givenOneInvalidField_whenValidating_thenReportsThatFieldWithItsMessage(String field, AddUnitsDto dto, String expectedMessage) {
        Map<String, Set<String>> messagesByField = validator.validate(dto).stream().collect(Collectors.groupingBy(
                violation -> violation.getPropertyPath().toString(),
                Collectors.mapping(ConstraintViolation::getMessage, Collectors.toSet())));

        assertThat(messagesByField).containsOnlyKeys(field);
        assertThat(messagesByField.get(field)).contains(expectedMessage);
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                Arguments.of("state", new AddUnitsDto(null, 1), "State is required"),
                Arguments.of("amount", new AddUnitsDto(ItemState.PRISTINE, null), "Amount is required"),
                Arguments.of("amount", new AddUnitsDto(ItemState.PRISTINE, 0), "Amount must be greater than 0"),
                Arguments.of("amount", new AddUnitsDto(ItemState.PRISTINE, 1001), "Amount must be at most 1000")
        );
    }
}
