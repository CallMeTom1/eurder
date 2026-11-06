package com.tbaar.eurder.feature.security.api.dto;

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

class CreateAdminDtoValidationTest {

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
    void givenCompleteAdmin_whenValidating_thenHasNoViolation() {
        CreateAdminDto dto = validAdmin();

        Set<ConstraintViolation<CreateAdminDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest(name = "{0} = \"{1}\" is rejected")
    @MethodSource("invalidFields")
    void givenOneInvalidField_whenValidating_thenReportsThatFieldWithItsMessage(String field, CreateAdminDto dto, String expectedMessage) {
        Map<String, Set<String>> messagesByField = messagesByField(validator.validate(dto));

        assertThat(messagesByField).containsOnlyKeys(field);
        assertThat(messagesByField.get(field)).contains(expectedMessage);
    }

    @Test
    void givenPasswordOfExactly72Characters_whenValidating_thenHasNoViolation() {
        CreateAdminDto dto = withPassword("a".repeat(72));

        Set<ConstraintViolation<CreateAdminDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                Arguments.of("email", withEmail(null), "Email is required"),
                Arguments.of("email", withEmail(" "), "Email is required"),
                Arguments.of("email", withEmail("jane"), "Email must be valid"),
                Arguments.of("email", withEmail("jane@"), "Email must be valid"),
                Arguments.of("email", withEmail("@doe.be"), "Email must be valid"),
                Arguments.of("email", withEmail("a".repeat(256)), "Email must be at most 255 characters"),
                Arguments.of("password", withPassword(null), "Password is required"),
                Arguments.of("password", withPassword("        "), "Password is required"),
                Arguments.of("password", withPassword("short"), "Password must be between 8 and 72 characters"),
                Arguments.of("password", withPassword("a".repeat(73)), "Password must be between 8 and 72 characters")
        );
    }

    private static Map<String, Set<String>> messagesByField(Set<ConstraintViolation<CreateAdminDto>> violations) {
        return violations.stream().collect(Collectors.groupingBy(
                violation -> violation.getPropertyPath().toString(),
                Collectors.mapping(ConstraintViolation::getMessage, Collectors.toSet())));
    }

    private static CreateAdminDto validAdmin() {
        return new CreateAdminDto("jane@doe.be", "secret123");
    }

    private static CreateAdminDto withEmail(String email) {
        return new CreateAdminDto(email, "secret123");
    }

    private static CreateAdminDto withPassword(String password) {
        return new CreateAdminDto("jane@doe.be", password);
    }
}
