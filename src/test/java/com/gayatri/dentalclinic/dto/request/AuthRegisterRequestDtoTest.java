package com.gayatri.dentalclinic.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class AuthRegisterRequestDtoTest {
    static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    @AfterAll static void close() { factory.close(); }

    static AuthRegisterRequestDto valid() {
        var dto = new AuthRegisterRequestDto();
        dto.setGender("Female"); dto.setFirstName("Ava"); dto.setLastName("Sharma");
        dto.setEmail("ava@example.com"); dto.setPhone("9876543210");
        dto.setPassword("password with spaces");
        return dto;
    }
    static boolean invalid(AuthRegisterRequestDto dto, String field) {
        return factory.getValidator().validate(dto).stream().anyMatch(v -> v.getPropertyPath().toString().equals(field));
    }
    @Test void normalizesProfileWithoutChangingPassword() {
        var dto = valid();
        dto.setFirstName("  राज  "); dto.setLastName(" O'Neil-Smith ");
        dto.setEmail(" Ava@EXAMPLE.COM "); dto.setPhone(" 9876543210 ");
        dto.setGender(" Female "); dto.setAddress("  "); dto.setPassword("  password  ");
        assertEquals("राज", dto.getFirstName()); assertEquals("O'Neil-Smith", dto.getLastName());
        assertEquals("ava@example.com", dto.getEmail()); assertEquals("9876543210", dto.getPhone());
        assertEquals("Female", dto.getGender()); assertNull(dto.getAddress()); assertEquals("  password  ", dto.getPassword());
        assertTrue(factory.getValidator().validate(dto).isEmpty());
    }
    @ParameterizedTest @ValueSource(strings = {"1231231223", "0123456789", "2234567890", "3234567890", "4234567890", "5234567890", "abcdefghij", "123456789", "12345678901", "+9876543210", "+919876543210", "1234567890123456", "98765 43210", "++919876543210", "          "})
    void rejectsInvalidPhone(String phone) {
        var dto = valid(); dto.setPhone(phone); assertTrue(invalid(dto, "phone"));
    }
    @ParameterizedTest @ValueSource(strings = {"6123456789", "7123456789", "8123456789", "9876543210"})
    void acceptsPhoneBoundaries(String phone) {
        var dto = valid(); dto.setPhone(phone); assertFalse(invalid(dto, "phone"));
    }
    @Test void validatesDatesAndGender() {
        var dto = valid(); dto.setDateOfBirth(LocalDate.now().plusDays(1)); dto.setGender("invalid");
        assertTrue(invalid(dto, "dateOfBirth")); assertTrue(invalid(dto, "gender"));
        dto.setDateOfBirth(LocalDate.now()); dto.setGender("Prefer not to say");
        assertTrue(factory.getValidator().validate(dto).isEmpty());
    }
    @Test void rejectsBlankAndOversizedProfileFields() {
        var dto = valid(); dto.setFirstName(" "); dto.setLastName("x".repeat(101));
        dto.setEmail("x".repeat(250) + "@example.com"); dto.setAddress("x".repeat(256));
        for (String field : new String[]{"firstName", "lastName", "email", "address"}) assertTrue(invalid(dto, field), field);
        dto.setFirstName("x".repeat(100)); dto.setLastName("x".repeat(100)); dto.setEmail("a@example.com"); dto.setAddress("12 Main Road, " + "x".repeat(241));
        assertTrue(factory.getValidator().validate(dto).isEmpty());
    }
    @Test void checksPasswordCharacterAndByteBoundaries() {
        var dto = valid(); dto.setPassword("1234567"); assertTrue(invalid(dto, "password"));
        dto.setPassword("        "); assertTrue(invalid(dto, "password"));
        dto.setPassword("12345678"); assertTrue(factory.getValidator().validate(dto).isEmpty());
        dto.setPassword("é".repeat(36)); assertTrue(factory.getValidator().validate(dto).isEmpty());
        dto.setPassword("é".repeat(37)); assertTrue(invalid(dto, "passwordWithinByteLimit"));
        dto.setPassword("x".repeat(73)); assertTrue(invalid(dto, "passwordWithinByteLimit"));
    }
    @Test void requiresGenderAndRejectsBirthDatesOlderThan100Years() {
        var dto = valid();
        for (String gender : new String[]{null, "", "   "}) {
            dto.setGender(gender);
            assertTrue(invalid(dto, "gender"));
        }
        dto.setGender("Female");
        LocalDate earliest = LocalDate.now().minusYears(100);
        dto.setDateOfBirth(earliest);
        assertTrue(factory.getValidator().validate(dto).isEmpty());
        dto.setDateOfBirth(earliest.minusDays(1));
        assertTrue(invalid(dto, "dateOfBirthWithinAgeLimit"));
        dto.setDateOfBirth(LocalDate.of(1, 10, 12));
        assertTrue(invalid(dto, "dateOfBirthWithinAgeLimit"));
        dto.setDateOfBirth(null);
        assertTrue(factory.getValidator().validate(dto).isEmpty());
    }
    @ParameterizedTest
    @ValueSource(strings = {"232131", "abc", "a12345", "१२३४", "---", "N/A", "unknown", "test", "qwerty", "aaaaaaaaaa", "a a a a a", "<script>alert(1)</script>", "user@example.com", "https://example.com", "www.example.com", "12 Main 😀 Road"})
    void rejectsIncompleteOrJunkAddresses(String address) {
        var dto = valid(); dto.setAddress(address);
        assertTrue(invalid(dto, "address"), address);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "23 MG Road, Pune 411001", "१२, पुणे", "Flat #2-B, O’Connell Road (Near St. Mary's)", "PO Box 123, Chennai", "House Rose, Village Rampur", "12 Main St.\nPune 411001", "東京市中央区1-2-3"})
    void acceptsCommonPostalAddressFormats(String address) {
        var dto = valid(); dto.setAddress(address);
        assertFalse(invalid(dto, "address"), address);
    }

    @Test void normalizesAddressWhitespaceAndRejectsEmbeddedControls() {
        var dto = valid();
        dto.setAddress("  Flat\t 2-B,\r\n  MG\u00a0 Road  ");
        assertEquals("Flat 2-B,\n MG Road", dto.getAddress());
        dto.setAddress("12 Main" + (char) 0 + " Road");
        assertTrue(invalid(dto, "address"));
    }
}
