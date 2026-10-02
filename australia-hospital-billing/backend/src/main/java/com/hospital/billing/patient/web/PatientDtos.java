package com.hospital.billing.patient.web;

import com.hospital.billing.patient.domain.Gender;
import com.hospital.billing.patient.domain.Patient;
import com.hospital.billing.patient.domain.PatientContact;
import com.hospital.billing.patient.domain.PatientStatus;
import com.hospital.billing.patient.validation.ValidMedicareNumber;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class PatientDtos {

    private static final String AU_PHONE = "^04[0-9]{8}$";
    private PatientDtos() {
    }

    public record PatientRequest(
            @NotBlank @Size(max = 60)
            String firstName,
            @NotBlank @Size(max = 60)
            String lastName,
            @NotNull @Past
            LocalDate dob,
            @NotNull Gender gender,
            @Pattern(regexp = AU_PHONE, message = "must be an Australian phone number")
            String phone,
            @Email @Size(max = 120)
            String email,
            @Size(max = 200)
            String address,
            @Size(max = 80)
            String suburb,
            @Pattern(regexp = "^(NSW|VIC|QLD|WA|SA|TAS|ACT|NT)$", message = "must be an Australian state code")
            String state,
            @Pattern(regexp = "^\\d{4}$", message = "must be a 4-digit postcode")
            String postcode,
            @ValidMedicareNumber
            String medicareNo,
            @Min(1) @Max(9)
            Short medicareIrn,
            PatientStatus status,
            @Valid List<ContactRequest> contacts) {
    }

    public record ContactRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 40) String relation,
            @NotBlank @Pattern(regexp = AU_PHONE, message = "must be an Australian phone number") String phone,
            @Email @Size(max = 120) String email,
            boolean primaryContact) {
    }

    public record ContactResponse(Long id, String name, String relation, String phone, String email,
                                  boolean primaryContact) {

        public static ContactResponse from(PatientContact c) {
            return new ContactResponse(c.getId(), c.getName(), c.getRelation(), c.getPhone(), c.getEmail(),
                    c.isPrimaryContact());
        }
    }

    /** Lightweight row for lists (no contacts: avoids N+1 queries). */
    public record PatientSummary(Long id, String mrn, String fullName, LocalDate dob, Gender gender,
                                 String phone, String medicareNo, PatientStatus status) {

        public static PatientSummary from(Patient p) {
            return new PatientSummary(p.getId(), p.getMrn(), p.fullName(), p.getDob(), p.getGender(),
                    p.getPhone(), p.getMedicareNo(), p.getStatus());
        }
    }

    public record PatientResponse(Long id, String mrn, String firstName, String lastName, String fullName,
                                  LocalDate dob, Gender gender, String phone, String email, String address,
                                  String suburb, String state, String postcode, String country,
                                  String medicareNo, Short medicareIrn, PatientStatus status,
                                  Instant createdAt, List<ContactResponse> contacts) {

        public static PatientResponse from(Patient p) {
            return new PatientResponse(p.getId(), p.getMrn(), p.getFirstName(), p.getLastName(), p.fullName(),
                    p.getDob(), p.getGender(), p.getPhone(), p.getEmail(), p.getAddress(), p.getSuburb(),
                    p.getState(), p.getPostcode(), p.getCountry(), p.getMedicareNo(), p.getMedicareIrn(),
                    p.getStatus(), p.getCreatedAt(),
                    p.getContacts().stream().map(ContactResponse::from).toList());
        }
    }
}
