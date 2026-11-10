package com.tbaar.eurder.common.api;

import com.tbaar.eurder.common.api.ValidationErrorMapper.ValidationError;
import com.tbaar.eurder.common.exception.ErrorCode;
import jakarta.validation.Constraint;
import jakarta.validation.Validation;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.util.ClassUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationErrorMapperTest {

    private static final SpringValidatorAdapter VALIDATOR = new SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void givenMissingValues_whenMapped_thenEveryKindOfRequiredGivesTheRequiredCode() {
        assertThat(errorsOf(new Required(null, " ", List.of()))).containsExactly(
                error("list", ErrorCode.VALIDATION_REQUIRED, Map.of()),
                error("object", ErrorCode.VALIDATION_REQUIRED, Map.of()),
                error("text", ErrorCode.VALIDATION_REQUIRED, Map.of()));
    }

    @Test
    void givenATextOutsideItsSize_whenMapped_thenSaysWhetherItIsTooShortOrTooLongWithTheBound() {
        assertThat(errorsOf(new Sized("abc", "abcdef", List.of(1, 2, 3)))).containsExactly(
                error("items", ErrorCode.VALIDATION_TOO_LONG, Map.of("max", 2)),
                error("tooLong", ErrorCode.VALIDATION_TOO_LONG, Map.of("max", 5)),
                error("tooShort", ErrorCode.VALIDATION_TOO_SHORT, Map.of("min", 4)));
    }

    @Test
    void givenNumbersOutOfRange_whenMapped_thenGivesTheCodeAndTheBoundOfEachConstraint() {
        assertThat(errorsOf(new Numbers(0, -1, 1001, new BigDecimal("123.456")))).containsExactly(
                error("amount", ErrorCode.VALIDATION_TOO_LARGE, Map.of("max", 1000L)),
                error("positive", ErrorCode.VALIDATION_MUST_BE_POSITIVE, Map.of()),
                error("positiveOrZero", ErrorCode.VALIDATION_MUST_NOT_BE_NEGATIVE, Map.of()),
                error("price", ErrorCode.VALIDATION_TOO_MANY_DIGITS, Map.of("integer", 8, "fraction", 2)));
    }

    @Test
    void givenBadlyFormattedValues_whenMapped_thenGivesTheInvalidFormatCode() {
        assertThat(errorsOf(new Formatted("not-an-email", "abc"))).containsExactly(
                error("email", ErrorCode.VALIDATION_INVALID_FORMAT, Map.of()),
                error("phone", ErrorCode.VALIDATION_INVALID_FORMAT, Map.of()));
    }

    @Test
    void givenAConstraintWithoutKey_whenMapped_thenFallsBackOnTheGenericInvalidCode() {
        assertThat(errorsOf(new Unmapped(false))).containsExactly(error("accepted", ErrorCode.VALIDATION_INVALID, Map.of()));
    }

    @Test
    void givenAFieldErrorNotComingFromBeanValidation_whenMapped_thenFallsBackOnTheGenericInvalidCode() {
        FieldError fieldError = new FieldError("dto", "name", "Name is wrong");

        assertThat(ValidationErrorMapper.toValidationErrors(List.of(fieldError)))
                .containsExactly(error("name", ErrorCode.VALIDATION_INVALID, Map.of()));
    }

    @Test
    void givenTheDtosOfTheApi_whenReadingTheirConstraints_thenEachOneHasItsOwnCode() {
        Set<Class<? extends Annotation>> constraints = dtoClasses()
                .flatMap(dto -> Arrays.stream(dto.getDeclaredFields()))
                .flatMap(ValidationErrorMapperTest::constraintsOf)
                .collect(Collectors.toSet());

        assertThat(constraints).isNotEmpty().allMatch(ValidationErrorMapper::isMapped, "has its own validation code");
    }

    private static List<ValidationError> errorsOf(Object target) {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "target");
        VALIDATOR.validate(target, bindingResult);
        return ValidationErrorMapper.toValidationErrors(bindingResult.getFieldErrors());
    }

    private static ValidationError error(String field, ErrorCode code, Map<String, Object> params) {
        return new ValidationError(field, code.key(), params);
    }

    private static Stream<Class<?>> dtoClasses() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter((reader, factory) -> reader.getClassMetadata().getClassName().endsWith("Dto"));
        return scanner.findCandidateComponents("com.tbaar.eurder").stream()
                .map(BeanDefinition::getBeanClassName)
                .map(name -> ClassUtils.resolveClassName(name, ValidationErrorMapperTest.class.getClassLoader()));
    }

    private static Stream<Class<? extends Annotation>> constraintsOf(Field field) {
        return Stream.concat(Arrays.stream(field.getAnnotations()), annotationsInside(field.getAnnotatedType()))
                .map(Annotation::annotationType)
                .filter(type -> type.isAnnotationPresent(Constraint.class));
    }

    private static Stream<Annotation> annotationsInside(AnnotatedType type) {
        Stream<Annotation> own = Arrays.stream(type.getAnnotations());
        if (type instanceof AnnotatedParameterizedType parameterized) {
            return Stream.concat(own, Arrays.stream(parameterized.getAnnotatedActualTypeArguments())
                    .flatMap(ValidationErrorMapperTest::annotationsInside));
        }
        return own;
    }

    private record Required(@NotNull Object object, @NotBlank String text, @NotEmpty List<String> list) {
    }

    private record Sized(@Size(min = 4) String tooShort, @Size(max = 5) String tooLong, @Size(max = 2) List<Integer> items) {
    }

    private record Numbers(@Positive int positive, @PositiveOrZero int positiveOrZero, @Max(1000) int amount,
                           @Digits(integer = 8, fraction = 2) BigDecimal price) {
    }

    private record Formatted(@Email String email, @Pattern(regexp = "[0-9]+") String phone) {
    }

    private record Unmapped(@AssertTrue boolean accepted) {
    }
}
