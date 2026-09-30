package com.hospital.billing.insurance.web;

import com.hospital.billing.insurance.domain.CoverTier;
import com.hospital.billing.insurance.domain.InsuranceCompany;
import com.hospital.billing.insurance.domain.InsurancePolicy;
import com.hospital.billing.insurance.domain.NetworkStatus;
import com.hospital.billing.insurance.domain.PayerType;
import com.hospital.billing.insurance.domain.PolicyStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class InsuranceDtos {

    private InsuranceDtos() {
    }

    public record CompanyRequest(
            @NotBlank @Size(max = 120) String companyName,
            @NotNull PayerType payerType,
            @NotNull NetworkStatus networkStatus,
            @DecimalMin("0.00") BigDecimal knownGapCap,
            @Pattern(regexp = "^\\d{11}$", message = "must be an 11-digit ABN") String abn,
            @Size(max = 120) String contactPerson,
            @Size(max = 20) String phone,
            @Email @Size(max = 120) String email,
            @Size(max = 200) String address) {
    }

    public record CompanyResponse(Long id, String companyName, PayerType payerType, NetworkStatus networkStatus,
                                  BigDecimal knownGapCap, String abn, String contactPerson, String phone,
                                  String email) {

        public static CompanyResponse from(InsuranceCompany c) {
            return new CompanyResponse(c.getId(), c.getCompanyName(), c.getPayerType(), c.getNetworkStatus(),
                    c.getKnownGapCap(), c.getAbn(), c.getContactPerson(), c.getPhone(), c.getEmail());
        }
    }

    public record PolicyRequest(
            @NotNull Long companyId,
            @NotBlank @Size(max = 40) String policyNo,
            @NotNull CoverTier coverTier,
            @NotNull @DecimalMin("0.00") BigDecimal excessAmount,
            @NotNull @DecimalMin("0.00") BigDecimal annualLimit,
            @NotNull LocalDate startDate,
            LocalDate endDate) {
    }

    public record PolicyStatusRequest(@NotNull PolicyStatus status) {
    }

    public record PolicyResponse(Long id, Long patientId, Long companyId, String companyName,
                                 NetworkStatus networkStatus, String policyNo, CoverTier coverTier,
                                 BigDecimal excessAmount, BigDecimal annualLimit, BigDecimal benefitsUsed,
                                 BigDecimal remainingLimit, LocalDate startDate, LocalDate endDate,
                                 PolicyStatus status) {

        public static PolicyResponse from(InsurancePolicy p) {
            return new PolicyResponse(p.getId(), p.getPatient().getId(), p.getCompany().getId(),
                    p.getCompany().getCompanyName(), p.getCompany().getNetworkStatus(), p.getPolicyNo(),
                    p.getCoverTier(), p.getExcessAmount(), p.getAnnualLimit(), p.getBenefitsUsed(),
                    p.remainingAnnualLimit(), p.getStartDate(), p.getEndDate(), p.getStatus());
        }
    }
}
