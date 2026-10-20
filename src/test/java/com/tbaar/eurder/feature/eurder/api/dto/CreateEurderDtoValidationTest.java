package com.tbaar.eurder.feature.eurder.api.dto;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CreateEurderDtoValidationTest {

    private static final UUID CHAIR_ID = UUID.fromString("3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90");

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
    void givenSeveralValidItemGroups_whenValidating_thenHasNoViolation() {
        CreateEurderDto dto = withItemGroups(new ItemGroupRequestDto(CHAIR_ID, 2), new ItemGroupRequestDto(CHAIR_ID, 1));

        Set<ConstraintViolation<CreateEurderDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest(name = "{0} is rejected with \"{2}\"")
    @MethodSource("invalidEurders")
    void givenInvalidEurder_whenValidating_thenReportsTheFieldWithItsMessage(String field, CreateEurderDto dto, String expectedMessage) {
        Map<String, Set<String>> messagesByField = messagesByField(validator.validate(dto));

        assertThat(messagesByField).containsOnlyKeys(field);
        assertThat(messagesByField.get(field)).contains(expectedMessage);
    }

    static Stream<Arguments> invalidEurders() {
        return Stream.of(
                Arguments.of("itemGroups", new CreateEurderDto(null), "At least one item group is required"),
                Arguments.of("itemGroups", withItemGroups(), "At least one item group is required"),
                Arguments.of("itemGroups[0].itemId", withItemGroups(new ItemGroupRequestDto(null, 2)), "Item id is required"),
                Arguments.of("itemGroups[0].amount", withItemGroups(new ItemGroupRequestDto(CHAIR_ID, null)), "Amount is required"),
                Arguments.of("itemGroups[0].amount", withItemGroups(new ItemGroupRequestDto(CHAIR_ID, 0)), "Amount must be greater than 0"),
                Arguments.of("itemGroups[1].amount", withItemGroups(new ItemGroupRequestDto(CHAIR_ID, 2), new ItemGroupRequestDto(CHAIR_ID, -1)), "Amount must be greater than 0"),
                Arguments.of("itemGroups[0].<list element>", withItemGroups((ItemGroupRequestDto) null), "Item group is required")
        );
    }

    private static Map<String, Set<String>> messagesByField(Set<ConstraintViolation<CreateEurderDto>> violations) {
        return violations.stream().collect(Collectors.groupingBy(
                violation -> violation.getPropertyPath().toString(),
                Collectors.mapping(ConstraintViolation::getMessage, Collectors.toSet())));
    }

    private static CreateEurderDto withItemGroups(ItemGroupRequestDto... itemGroups) {
        List<ItemGroupRequestDto> list = new ArrayList<>(Arrays.asList(itemGroups));
        return new CreateEurderDto(list);
    }
}
