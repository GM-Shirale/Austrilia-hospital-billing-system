package com.hospital.billing.patient.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicareNumberValidatorTest {

    @Test
    void acceptsNumbersWithCorrectCheckDigit() {
        assertTrue(MedicareNumberValidator.isValid("2123456071"));
        assertTrue(MedicareNumberValidator.isValid("3987654081"));
    }

    @Test
    void rejectsWrongCheckDigit() {
        assertFalse(MedicareNumberValidator.isValid("2123456081"));
    }

    @Test
    void rejectsWrongLengthOrFirstDigit() {
        assertFalse(MedicareNumberValidator.isValid("212345607"));
        assertFalse(MedicareNumberValidator.isValid("1123456071"));
        assertFalse(MedicareNumberValidator.isValid(null));
    }

    @Test
    void blankIsAllowedBecauseMedicareIsOptional() {
        assertTrue(new MedicareNumberValidator().isValid(null, null));
        assertTrue(new MedicareNumberValidator().isValid("", null));
    }
}
