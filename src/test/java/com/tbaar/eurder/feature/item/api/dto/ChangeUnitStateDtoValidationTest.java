package com.tbaar.eurder.feature.item.api.dto;

import com.tbaar.eurder.feature.item.domain.ItemState;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ChangeUnitStateDtoValidationTest {

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
    void givenAState_whenValidating_thenHasNoViolation() {
        ChangeUnitStateDto dto = new ChangeUnitStateDto(ItemState.SLIGHTLY_DAMAGED);

        Set<ConstraintViolation<ChangeUnitStateDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void givenNoState_whenValidating_thenReportsThatTheStateIsRequired() {
        ChangeUnitStateDto dto = new ChangeUnitStateDto(null);

        Set<ConstraintViolation<ChangeUnitStateDto>> violations = validator.validate(dto);

        assertThat(violations).extracting(ConstraintViolation::getMessage).containsExactly("State is required");
    }
}
