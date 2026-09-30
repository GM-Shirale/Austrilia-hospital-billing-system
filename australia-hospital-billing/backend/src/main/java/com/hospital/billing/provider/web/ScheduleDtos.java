package com.hospital.billing.provider.web;

import com.hospital.billing.provider.domain.DoctorSchedule;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalTime;

public final class ScheduleDtos {

    private ScheduleDtos() {
    }

    public record ScheduleRequest(
            @NotNull(message = "Day is required") DayOfWeek dayOfWeek,
            @NotNull(message = "Start time is required") LocalTime startTime,
            @NotNull(message = "End time is required") LocalTime endTime,
            @Min(value = 5, message = "Slot must be at least 5 minutes")
            @Max(value = 120, message = "Slot cannot exceed 120 minutes") Integer slotMinutes,
            @Size(max = 100) String location) {
    }

    public record ScheduleResponse(Long id, Long doctorId, String doctorName, String departmentName,
                                   DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                                   int slotMinutes, String location) {

        public static ScheduleResponse from(DoctorSchedule s) {
            return new ScheduleResponse(s.getId(), s.getDoctor().getId(), s.getDoctor().displayName(),
                    s.getDoctor().getDepartment().getDepartmentName(), s.getDayOfWeek(), s.getStartTime(),
                    s.getEndTime(), s.getSlotMinutes(), s.getLocation());
        }
    }
}
