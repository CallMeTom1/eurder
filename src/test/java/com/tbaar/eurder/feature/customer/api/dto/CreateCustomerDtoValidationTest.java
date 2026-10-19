package com.tbaar.eurder.feature.customer.api.dto;

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

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CreateCustomerDtoValidationTest {

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
    void givenCompleteCustomer_whenValidating_thenHasNoViolation() {
        CreateCustomerDto dto = validCustomer();

        Set<ConstraintViolation<CreateCustomerDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest(name = "{0} = \"{1}\" is rejected")
    @MethodSource("invalidFields")
    void givenOneInvalidField_whenValidating_thenReportsThatFieldWithItsMessage(String field, CreateCustomerDto dto, String expectedMessage) {
        Map<String, Set<String>> messagesByField = messagesByField(validator.validate(dto));

        assertThat(messagesByField).containsOnlyKeys(field);
        assertThat(messagesByField.get(field)).contains(expectedMessage);
    }

    @ParameterizedTest(name = "phone number \"{0}\" is accepted")
    @ValueSource(strings = {"0470123456", "+32 470 12 34 56", "0470/12.34.56", "02-123-45-67"})
    void givenBelgianPhoneNumberFormat_whenValidating_thenHasNoViolation(String phoneNumber) {
        CreateCustomerDto dto = withPhoneNumber(phoneNumber);

        Set<ConstraintViolation<CreateCustomerDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void givenPasswordOfExactly72Characters_whenValidating_thenHasNoViolation() {
        CreateCustomerDto dto = withPassword("a".repeat(72));

        Set<ConstraintViolation<CreateCustomerDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                Arguments.of("firstName", withFirstName(" "), "First name is required"),
                Arguments.of("firstName", withFirstName("a".repeat(256)), "First name must be at most 255 characters"),
                Arguments.of("lastName", withLastName(""), "Last name is required"),
                Arguments.of("lastName", withLastName("a".repeat(256)), "Last name must be at most 255 characters"),
                Arguments.of("email", withEmail(null), "Email is required"),
                Arguments.of("email", withEmail("jane"), "Email must be valid"),
                Arguments.of("email", withEmail("jane@"), "Email must be valid"),
                Arguments.of("email", withEmail("@doe.be"), "Email must be valid"),
                Arguments.of("password", withPassword(null), "Password is required"),
                Arguments.of("password", withPassword("        "), "Password is required"),
                Arguments.of("password", withPassword("short"), "Password must be between 8 and 72 characters"),
                Arguments.of("password", withPassword("a".repeat(73)), "Password must be between 8 and 72 characters"),
                Arguments.of("phoneNumber", withPhoneNumber(null), "Phone number is required"),
                Arguments.of("phoneNumber", withPhoneNumber("abc"), "Phone number must be valid"),
                Arguments.of("address", withAddress(null), "Address is required"),
                Arguments.of("address.street", withAddress(new AddressDto(" ", "1", "1000", "Bruxelles")), "Street is required"),
                Arguments.of("address.houseNumber", withAddress(new AddressDto("Rue Haute", "", "1000", "Bruxelles")), "House number is required"),
                Arguments.of("address.houseNumber", withAddress(new AddressDto("Rue Haute", "1".repeat(21), "1000", "Bruxelles")), "House number must be at most 20 characters"),
                Arguments.of("address.postalCode", withAddress(new AddressDto("Rue Haute", "1", null, "Bruxelles")), "Postal code is required"),
                Arguments.of("address.city", withAddress(new AddressDto("Rue Haute", "1", "1000", "  ")), "City is required")
        );
    }

    private static Map<String, Set<String>> messagesByField(Set<ConstraintViolation<CreateCustomerDto>> violations) {
        return violations.stream().collect(Collectors.groupingBy(
                violation -> violation.getPropertyPath().toString(),
                Collectors.mapping(ConstraintViolation::getMessage, Collectors.toSet())));
    }

    private static CreateCustomerDto validCustomer() {
        return new CreateCustomerDto("Jane", "Doe", "jane@doe.be", "secret123", validAddress(), "0470123456");
    }

    private static AddressDto validAddress() {
        return new AddressDto("Rue Haute", "1", "1000", "Bruxelles");
    }

    private static CreateCustomerDto withFirstName(String firstName) {
        return new CreateCustomerDto(firstName, "Doe", "jane@doe.be", "secret123", validAddress(), "0470123456");
    }

    private static CreateCustomerDto withLastName(String lastName) {
        return new CreateCustomerDto("Jane", lastName, "jane@doe.be", "secret123", validAddress(), "0470123456");
    }

    private static CreateCustomerDto withEmail(String email) {
        return new CreateCustomerDto("Jane", "Doe", email, "secret123", validAddress(), "0470123456");
    }

    private static CreateCustomerDto withPassword(String password) {
        return new CreateCustomerDto("Jane", "Doe", "jane@doe.be", password, validAddress(), "0470123456");
    }

    private static CreateCustomerDto withAddress(AddressDto address) {
        return new CreateCustomerDto("Jane", "Doe", "jane@doe.be", "secret123", address, "0470123456");
    }

    private static CreateCustomerDto withPhoneNumber(String phoneNumber) {
        return new CreateCustomerDto("Jane", "Doe", "jane@doe.be", "secret123", validAddress(), phoneNumber);
    }
}
