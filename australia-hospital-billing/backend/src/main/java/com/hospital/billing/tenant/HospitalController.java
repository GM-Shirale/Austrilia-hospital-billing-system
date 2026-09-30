package com.hospital.billing.tenant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Hospitals (tenants)")
@RestController
@RequestMapping("/api/v1/hospitals")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalRepository hospitalRepository;

    public record HospitalResponse(UUID id, String code, String name, HospitalType hospitalType, String state) {
    }

    @Operation(summary = "Active hospitals, used by the login screen (public)")
    @GetMapping
    public List<HospitalResponse> list() {
        return hospitalRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(h -> new HospitalResponse(h.getId(), h.getCode(), h.getName(), h.getHospitalType(), h.getState()))
                .toList();
    }
}
