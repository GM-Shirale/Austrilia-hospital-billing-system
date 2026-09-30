package com.hospital.billing.consultation.service;

import org.springframework.context.ApplicationEventPublisher;
import com.hospital.billing.common.event.ChargeSourceChangedEvent;
import com.hospital.billing.admission.domain.Admission;
import com.hospital.billing.admission.service.AdmissionReadOperations;
import com.hospital.billing.common.domain.DocumentNumberGenerator;
import com.hospital.billing.common.domain.DocumentType;
import com.hospital.billing.common.exception.BusinessRuleException;
import com.hospital.billing.common.exception.EntityNotFoundException;
import com.hospital.billing.common.money.Money;
import com.hospital.billing.consultation.domain.Consultation;
import com.hospital.billing.consultation.domain.ConsultationRepository;
import com.hospital.billing.consultation.domain.ConsultationStatus;
import com.hospital.billing.consultation.web.ConsultationDtos.ConsultationRequest;
import com.hospital.billing.consultation.web.ConsultationDtos.ConsultationResponse;
import com.hospital.billing.provider.domain.Doctor;
import com.hospital.billing.provider.service.DoctorScheduleService;
import com.hospital.billing.provider.service.ProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final AdmissionReadOperations admissionReads;
    private final ProviderService providerService;
    private final DoctorScheduleService scheduleService;
    private final DocumentNumberGenerator documentNumberGenerator;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public List<ConsultationResponse> forAdmission(Long admissionId) {
        return consultationRepository.findByAdmissionIdOrderByConsultationAtAsc(admissionId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Only COMPLETED consultations are billed (used by the DoctorChargeCollector). */
    public List<Consultation> billableConsultations(Long admissionId) {
        return consultationRepository.findByAdmissionIdAndStatusOrderByConsultationAtAsc(admissionId,
                ConsultationStatus.COMPLETED);
    }

    @Transactional
    public ConsultationResponse record(Long admissionId, ConsultationRequest request) {
        Admission admission = admissionReads.requireAdmission(admissionId);
        admission.requireActive();
        Doctor doctor = providerService.requireActiveDoctor(request.doctorId());

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime at = request.consultationAt() != null ? request.consultationAt() : now;
        if (at.isBefore(admission.getAdmissionDate())) {
            throw new BusinessRuleException("A consultation cannot be before the admission date");
        }
        if (at.isAfter(now.plusHours(1))) {
            throw new BusinessRuleException("Only consultations that have taken place can be recorded");
        }

        Consultation consultation = new Consultation();
        consultation.setConsultationNo(documentNumberGenerator.next(DocumentType.CONSULTATION));
        consultation.setAdmission(admission);
        consultation.setDoctor(doctor);
        consultation.setConsultationType(request.consultationType());
        consultation.setConsultationAt(at);
        consultation.setMbsItemNo(request.mbsItemNo() != null && !request.mbsItemNo().isBlank()
                ? request.mbsItemNo().trim() : doctor.getMbsItemNo());
        consultation.setMbsScheduleFee(Money.of(request.mbsScheduleFee() != null
                ? request.mbsScheduleFee() : doctor.getMbsScheduleFee()));
        consultation.setFee(Money.of(request.fee() != null ? request.fee() : doctor.getConsultationFee()));
        consultation.setNotes(request.notes());
        Consultation saved = consultationRepository.save(consultation);
        events.publishEvent(new ChargeSourceChangedEvent(admission.getId(), "Consultation " + saved.getConsultationNo()));
        log.info("Recorded consultation {} by {} on admission {}", saved.getConsultationNo(), doctor.displayName(),
                admission.getAdmissionNo());
        return toResponse(saved);
    }

    @Transactional
    public ConsultationResponse cancel(Long consultationId) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new EntityNotFoundException("Consultation", consultationId));
        consultation.cancel();
        events.publishEvent(new ChargeSourceChangedEvent(consultation.getAdmission().getId(),
                "Consultation " + consultation.getConsultationNo() + " cancelled"));
        return toResponse(consultation);
    }

    private ConsultationResponse toResponse(Consultation c) {
        return ConsultationResponse.from(c, scheduleService.isRostered(c.getDoctor().getId(), c.getConsultationAt()));
    }
}
