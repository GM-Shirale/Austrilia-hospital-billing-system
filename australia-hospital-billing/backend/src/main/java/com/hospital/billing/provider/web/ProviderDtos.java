package com.hospital.billing.provider.web;

import com.hospital.billing.provider.domain.Department;
import com.hospital.billing.provider.domain.Doctor;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;

public final class ProviderDtos {

    private ProviderDtos() {
    }

    public record DepartmentRequest(
            @NotBlank @Size(max = 100) String departmentName,
            @Size(max = 100) String location) {
    }

    /** Serializable because department lists are cached (Redis profile). */
    public record DepartmentResponse(Long id, String departmentName, String location) implements Serializable {

        public static DepartmentResponse from(Department d) {
            return new DepartmentResponse(d.getId(), d.getDepartmentName(), d.getLocation());
        }
    }

    public record DoctorRequest(
            @NotNull Long departmentId,
            @NotBlank @Size(max = 60) String firstName,
            @NotBlank @Size(max = 60) String lastName,
            @NotBlank @Size(max = 100) String specialization,
            @Pattern(regexp = "^(\\+61|0)[2-478]\\d{8}$", message = "must be an Australian phone number") String phone,
            @Email @Size(max = 120) String email,
            @NotBlank
            @Pattern(regexp = "^\\d{6}[0-9A-Z][A-Z]$", message = "must be a Medicare provider number, e.g. 2451731J")
            String providerNo,
            @NotNull @DecimalMin("0.00") BigDecimal consultationFee,
            @NotBlank @Size(max = 10) String mbsItemNo,
            @NotNull @DecimalMin("0.00") BigDecimal mbsScheduleFee,
            Boolean active) {
    }

    public record DoctorResponse(Long id, String firstName, String lastName, String displayName,
                                 String specialization, Long departmentId, String departmentName,
                                 String phone, String email, String providerNo, BigDecimal consultationFee,
                                 String mbsItemNo, BigDecimal mbsScheduleFee, boolean active) {

        public static DoctorResponse from(Doctor d) {
            return new DoctorResponse(d.getId(), d.getFirstName(), d.getLastName(), d.displayName(),
                    d.getSpecialization(), d.getDepartment().getId(), d.getDepartment().getDepartmentName(),
                    d.getPhone(), d.getEmail(), d.getProviderNo(), d.getConsultationFee(), d.getMbsItemNo(),
                    d.getMbsScheduleFee(), d.isActive());
        }
    }
}
