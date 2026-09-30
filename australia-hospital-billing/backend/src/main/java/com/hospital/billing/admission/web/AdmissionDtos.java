package com.hospital.billing.admission.web;

import com.hospital.billing.admission.domain.Admission;
import com.hospital.billing.admission.domain.AdmissionStatus;
import com.hospital.billing.admission.domain.AdmissionType;
import com.hospital.billing.admission.domain.CareSetting;
import com.hospital.billing.admission.domain.FinancialClass;
import com.hospital.billing.admission.domain.Room;
import com.hospital.billing.admission.domain.RoomAllocation;
import com.hospital.billing.admission.domain.RoomStatus;
import com.hospital.billing.admission.domain.RoomType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class AdmissionDtos {

    private AdmissionDtos() {
    }

    public record AdmitRequest(
            @NotNull Long patientId,
            @NotNull Long doctorId,
            @NotNull Long admissionTypeId,
            @NotNull FinancialClass financialClass,
            CareSetting careSetting,
            LocalDateTime admissionDate,
            @Size(max = 500) String diagnosis,
            Long roomId) {
    }

    public record ChangeDoctorRequest(@NotNull Long doctorId) {
    }

    public record DischargeRequest(LocalDateTime dischargeDate) {
    }

    public record BedAllocationRequest(@NotNull Long roomId, LocalDateTime fromDate) {
    }

    public record AdmissionTypeResponse(Long id, String code, String typeName) implements Serializable {

        public static AdmissionTypeResponse from(AdmissionType t) {
            return new AdmissionTypeResponse(t.getId(), t.getCode(), t.getTypeName());
        }
    }

    public record AdmissionSummary(Long id, String admissionNo, Long patientId, String patientName, String patientMrn,
                                   Long doctorId, String doctorName, String admissionType,
                                   FinancialClass financialClass, CareSetting careSetting,
                                   LocalDateTime admissionDate, LocalDateTime dischargeDate,
                                   AdmissionStatus status, long lengthOfStayDays) {

        public static AdmissionSummary from(Admission a) {
            return new AdmissionSummary(a.getId(), a.getAdmissionNo(), a.getPatient().getId(),
                    a.getPatient().fullName(), a.getPatient().getMrn(), a.getDoctor().getId(),
                    a.getDoctor().displayName(), a.getAdmissionType().getTypeName(), a.getFinancialClass(),
                    a.getCareSetting(), a.getAdmissionDate(), a.getDischargeDate(), a.getStatus(),
                    a.lengthOfStayDays(LocalDateTime.now()));
        }
    }

    public record AdmissionResponse(AdmissionSummary admission, String diagnosis, String departmentName,
                                    String patientMedicareNo, BedAllocationResponse currentBed,
                                    List<BedAllocationResponse> bedHistory, String doctorProviderNo) {
    }

    public record BedAllocationResponse(Long id, Long roomId, String roomNo, String bedNo, RoomType roomType,
                                        LocalDateTime fromDate, LocalDateTime toDate, BigDecimal chargePerDay) {

        public static BedAllocationResponse from(RoomAllocation a) {
            Room r = a.getRoom();
            return new BedAllocationResponse(a.getId(), r.getId(), r.getRoomNo(), r.getBedNo(), r.getRoomType(),
                    a.getFromDate(), a.getToDate(), a.getChargePerDay());
        }
    }

    public record RoomRequest(
            @NotBlank @Size(max = 20) String roomNo,
            @NotBlank @Size(max = 10) String bedNo,
            @NotNull RoomType roomType,
            @NotNull @DecimalMin("0.00") BigDecimal dailyRate,
            RoomStatus status) {
    }

    public record RoomResponse(Long id, String roomNo, String bedNo, RoomType roomType, BigDecimal dailyRate,
                               RoomStatus status) {

        public static RoomResponse from(Room r) {
            return new RoomResponse(r.getId(), r.getRoomNo(), r.getBedNo(), r.getRoomType(), r.getDailyRate(),
                    r.getStatus());
        }
    }
}
