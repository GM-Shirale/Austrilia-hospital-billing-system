package com.hospital.billing.provider.web;

import com.hospital.billing.provider.service.DoctorScheduleService;
import com.hospital.billing.provider.web.ScheduleDtos.ScheduleRequest;
import com.hospital.billing.provider.web.ScheduleDtos.ScheduleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Doctors & Departments")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DoctorScheduleController {

    private static final String CAN_MANAGE = "hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR')";

    private final DoctorScheduleService scheduleService;

    @Operation(summary = "Weekly roster of every doctor")
    @GetMapping("/doctor-schedules")
    public List<ScheduleResponse> all() {
        return scheduleService.all();
    }

    @GetMapping("/doctors/{doctorId}/schedules")
    public List<ScheduleResponse> forDoctor(@PathVariable Long doctorId) {
        return scheduleService.forDoctor(doctorId);
    }

    @PostMapping("/doctors/{doctorId}/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(CAN_MANAGE)
    public ScheduleResponse create(@PathVariable Long doctorId, @Valid @RequestBody ScheduleRequest request) {
        return scheduleService.create(doctorId, request);
    }

    @PutMapping("/doctor-schedules/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ScheduleResponse update(@PathVariable Long id, @Valid @RequestBody ScheduleRequest request) {
        return scheduleService.update(id, request);
    }

    @DeleteMapping("/doctor-schedules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(CAN_MANAGE)
    public void delete(@PathVariable Long id) {
        scheduleService.delete(id);
    }
}
