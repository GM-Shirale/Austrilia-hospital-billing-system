package com.hospital.billing.patient.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Medicare card number check-digit algorithm:
 * <ul>
 *   <li>10 digits; the first digit is 2-6;</li>
 *   <li>digit 9 = (d1*1 + d2*3 + d3*7 + d4*9 + d5*1 + d6*3 + d7*7 + d8*9) mod 10;</li>
 *   <li>digit 10 is the card issue number.</li>
 * </ul>
 * Catching typos at registration avoids Medicare claim rejections weeks later.
 */
public class MedicareNumberValidator implements ConstraintValidator<ValidMedicareNumber, String> {

    private static final int[] WEIGHTS = {1, 3, 7, 9, 1, 3, 7, 9};

    public static boolean isValid(String value) {
        if (value == null || !value.matches("^[2-6]\\d{9}$")) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += Character.getNumericValue(value.charAt(i)) * WEIGHTS[i];
        }
        return sum % 10 == Character.getNumericValue(value.charAt(8));
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.isBlank() || isValid(value.replace(" ", ""));
    }
}
