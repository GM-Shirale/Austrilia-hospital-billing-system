package com.hospital.billing.consultation.web;

import com.hospital.billing.consultation.service.ConsultationService;
import com.hospital.billing.consultation.web.ConsultationDtos.ConsultationRequest;
import com.hospital.billing.consultation.web.ConsultationDtos.ConsultationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Consultations")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;

    @GetMapping("/admissions/{admissionId}/consultations")
    public List<ConsultationResponse> forAdmission(@PathVariable Long admissionId) {
        return consultationService.forAdmission(admissionId);
    }

    @Operation(summary = "Record a doctor consultation on an ACTIVE admission (billed with its MBS item)")
    @PostMapping("/admissions/{admissionId}/consultations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','RECEPTIONIST')")
    public ConsultationResponse record(@PathVariable Long admissionId,
                                       @Valid @RequestBody ConsultationRequest request) {
        return consultationService.record(admissionId, request);
    }

    @PostMapping("/consultations/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public ConsultationResponse cancel(@PathVariable Long id) {
        return consultationService.cancel(id);
    }
}
