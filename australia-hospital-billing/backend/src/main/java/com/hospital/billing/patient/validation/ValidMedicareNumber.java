package com.hospital.billing.patient.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Validates an Australian Medicare card number (10 digits incl. check digit). Null is valid. */
@Documented
@Constraint(validatedBy = MedicareNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMedicareNumber {

    String message() default "is not a valid Medicare card number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
