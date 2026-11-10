package com.tbaar.eurder.common.api;

import com.tbaar.eurder.common.exception.ErrorCode;
import jakarta.validation.ConstraintViolation;
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
import org.springframework.validation.FieldError;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;

/**
 * Turns each invalid field into a translatable {@link ErrorCode}: the key depends on the kind of constraint,
 * so the DTOs keep their annotations as they are and a new DTO needs no extra work.
 */
final class ValidationErrorMapper {

    /**
     * Constraints whose key needs no value of the annotation. {@link Size}, {@link Max} and {@link Digits} are mapped apart.
     */
    static final Map<Class<? extends Annotation>, ErrorCode> CODES_BY_CONSTRAINT = Map.ofEntries(
            entry(NotNull.class, ErrorCode.VALIDATION_REQUIRED),
            entry(NotBlank.class, ErrorCode.VALIDATION_REQUIRED),
            entry(NotEmpty.class, ErrorCode.VALIDATION_REQUIRED),
            entry(Email.class, ErrorCode.VALIDATION_INVALID_FORMAT),
            entry(Pattern.class, ErrorCode.VALIDATION_INVALID_FORMAT),
            entry(Positive.class, ErrorCode.VALIDATION_MUST_BE_POSITIVE),
            entry(PositiveOrZero.class, ErrorCode.VALIDATION_MUST_NOT_BE_NEGATIVE));

    private ValidationErrorMapper() {
    }

    static List<ValidationError> toValidationErrors(List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .map(ValidationErrorMapper::toValidationError)
                .sorted(Comparator.comparing(ValidationError::field).thenComparing(ValidationError::code))
                .toList();
    }

    static boolean isMapped(Class<? extends Annotation> constraint) {
        return CODES_BY_CONSTRAINT.containsKey(constraint)
                || constraint == Size.class || constraint == Max.class || constraint == Digits.class;
    }

    private static ValidationError toValidationError(FieldError fieldError) {
        return violationOf(fieldError)
                .map(violation -> fromViolation(fieldError.getField(), violation))
                .orElseGet(() -> ValidationError.of(fieldError.getField(), ErrorCode.VALIDATION_INVALID, Map.of()));
    }

    private static Optional<ConstraintViolation<?>> violationOf(FieldError fieldError) {
        return fieldError.contains(ConstraintViolation.class)
                ? Optional.of(fieldError.unwrap(ConstraintViolation.class))
                : Optional.empty();
    }

    private static ValidationError fromViolation(String field, ConstraintViolation<?> violation) {
        Annotation constraint = violation.getConstraintDescriptor().getAnnotation();
        if (constraint instanceof Size size) {
            return sizeError(field, size, violation.getInvalidValue());
        }
        if (constraint instanceof Max max) {
            return ValidationError.of(field, ErrorCode.VALIDATION_TOO_LARGE, Map.of("max", max.value()));
        }
        if (constraint instanceof Digits digits) {
            return ValidationError.of(field, ErrorCode.VALIDATION_TOO_MANY_DIGITS,
                    Map.of("integer", digits.integer(), "fraction", digits.fraction()));
        }
        return ValidationError.of(field, CODES_BY_CONSTRAINT.getOrDefault(constraint.annotationType(), ErrorCode.VALIDATION_INVALID), Map.of());
    }

    private static ValidationError sizeError(String field, Size size, Object invalidValue) {
        return lengthOf(invalidValue) < size.min()
                ? ValidationError.of(field, ErrorCode.VALIDATION_TOO_SHORT, Map.of("min", size.min()))
                : ValidationError.of(field, ErrorCode.VALIDATION_TOO_LONG, Map.of("max", size.max()));
    }

    private static int lengthOf(Object value) {
        if (value instanceof CharSequence text) {
            return text.length();
        }
        if (value instanceof Collection<?> collection) {
            return collection.size();
        }
        if (value instanceof Map<?, ?> map) {
            return map.size();
        }
        return value != null && value.getClass().isArray() ? Array.getLength(value) : 0;
    }

    /**
     * One invalid field: its path in the body, the key of the broken constraint and the values the translation needs.
     */
    record ValidationError(String field, String code, Map<String, Object> params) {

        static ValidationError of(String field, ErrorCode code, Map<String, Object> params) {
            return new ValidationError(field, code.key(), params);
        }
    }
}
