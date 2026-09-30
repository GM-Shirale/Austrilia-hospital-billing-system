package com.hospital.billing.admission.service;

import com.hospital.billing.common.event.ChargeSourceChangedEvent;
import com.hospital.billing.admission.domain.Admission;
import com.hospital.billing.admission.domain.AdmissionRepository;
import com.hospital.billing.admission.domain.AdmissionStatus;
import com.hospital.billing.admission.domain.AdmissionType;
import com.hospital.billing.admission.domain.AdmissionTypeRepository;
import com.hospital.billing.admission.domain.CareSetting;
import com.hospital.billing.admission.event.AdmissionDischargedEvent;
import com.hospital.billing.admission.web.AdmissionDtos.ChangeDoctorRequest;
import com.hospital.billing.admission.web.AdmissionDtos.AdmissionResponse;
import com.hospital.billing.admission.web.AdmissionDtos.AdmissionSummary;
import com.hospital.billing.admission.web.AdmissionDtos.AdmissionTypeResponse;
import com.hospital.billing.admission.web.AdmissionDtos.AdmitRequest;
import com.hospital.billing.admission.web.AdmissionDtos.BedAllocationRequest;
import com.hospital.billing.admission.web.AdmissionDtos.BedAllocationResponse;
import com.hospital.billing.admission.web.AdmissionDtos.DischargeRequest;
import com.hospital.billing.common.domain.DocumentNumberGenerator;
import com.hospital.billing.common.domain.DocumentType;
import com.hospital.billing.common.exception.BusinessRuleException;
import com.hospital.billing.common.exception.EntityNotFoundException;
import com.hospital.billing.common.web.PageResponse;
import com.hospital.billing.patient.domain.Patient;
import com.hospital.billing.patient.domain.PatientStatus;
import com.hospital.billing.patient.service.PatientService;
import com.hospital.billing.provider.domain.Doctor;
import com.hospital.billing.provider.service.ProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implements both the read and the lifecycle contracts. Bed management is delegated to
 * {@link BedAllocationOperations} (Single Responsibility).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdmissionService implements AdmissionReadOperations, AdmissionLifecycleOperations {

    private final AdmissionRepository admissionRepository;
    private final AdmissionTypeRepository admissionTypeRepository;
    private final PatientService patientService;
    private final ProviderService providerService;
    private final BedAllocationOperations bedAllocationOperations;
    private final DocumentNumberGenerator documentNumberGenerator;
    private final ApplicationEventPublisher applicationEvents;
    private final Clock clock;

    // ---------------------------------------------------------------- reads

    @Override
    public PageResponse<AdmissionSummary> search(AdmissionStatus status, Pageable pageable) {
        Page<Admission> page = status == null
                ? admissionRepository.findAll(pageable)
                : admissionRepository.findByStatus(status, pageable);
        return PageResponse.from(page.map(AdmissionSummary::from));
    }

    @Override
    public AdmissionResponse get(Long admissionId) {
        return toResponse(requireAdmission(admissionId));
    }

    @Override
    public List<AdmissionSummary> findByPatient(Long patientId) {
        return admissionRepository.findByPatientIdOrderByAdmissionDateDesc(patientId).stream()
                .map(AdmissionSummary::from)
                .toList();
    }

    @Override
    @Cacheable(cacheNames = "admissionTypes", key = "T(com.hospital.billing.tenant.TenantContext).requireCurrentTenant()")
    public List<AdmissionTypeResponse> admissionTypes() {
        return admissionTypeRepository.findAllByOrderByTypeNameAsc().stream()
                .map(AdmissionTypeResponse::from)
                .toList();
    }

    @Override
    public Admission requireAdmission(Long admissionId) {
        return admissionRepository.findWithDetailsById(admissionId)
                .orElseThrow(() -> new EntityNotFoundException("Admission", admissionId));
    }

    // ------------------------------------------------------------ lifecycle

    @Override
    @Transactional
    public AdmissionResponse admit(AdmitRequest request) {
        Patient patient = patientService.requirePatient(request.patientId());
        if (patient.getStatus() != PatientStatus.ACTIVE) {
            throw new BusinessRuleException("Patient %s is %s and cannot be admitted"
                    .formatted(patient.getMrn(), patient.getStatus()));
        }
        if (admissionRepository.existsByPatientIdAndStatus(patient.getId(), AdmissionStatus.ADMITTED)) {
            throw new BusinessRuleException("Patient %s is already admitted".formatted(patient.getMrn()));
        }
        Doctor doctor = providerService.requireActiveDoctor(request.doctorId());
        AdmissionType type = admissionTypeRepository.findById(request.admissionTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Admission type", request.admissionTypeId()));

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime admissionDate = request.admissionDate() != null ? request.admissionDate() : now;
        if (admissionDate.isAfter(now.plusDays(1))) {
            throw new BusinessRuleException("Admission date cannot be in the future");
        }

        CareSetting careSetting = request.careSetting() == null ? CareSetting.IPD : request.careSetting();
        if (!careSetting.requiresBed() && request.roomId() != null) {
            throw new BusinessRuleException("Out-patient (OPD) visits do not occupy a bed; remove the bed selection");
        }

        Admission admission = new Admission();
        admission.setAdmissionNo(documentNumberGenerator.next(DocumentType.ADMISSION));
        admission.setPatient(patient);
        admission.setDoctor(doctor);
        admission.setAdmissionType(type);
        admission.setFinancialClass(request.financialClass());
        admission.setCareSetting(careSetting);
        admission.setAdmissionDate(admissionDate);
        admission.setDiagnosis(request.diagnosis());
        Admission saved = admissionRepository.save(admission);

        if (request.roomId() != null) {
            bedAllocationOperations.allocateBed(saved.getId(), new BedAllocationRequest(request.roomId(), admissionDate));
        }
        log.info("Admitted patient {} as {}", patient.getMrn(), saved.getAdmissionNo());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AdmissionResponse discharge(Long admissionId, DischargeRequest request) {
        Admission admission = requireAdmission(admissionId);
        LocalDateTime at = request != null && request.dischargeDate() != null
                ? request.dischargeDate()
                : LocalDateTime.now(clock);
        admission.discharge(at);
        bedAllocationOperations.releaseBed(admissionId, at);   // bed -> AVAILABLE
        // Same transaction, synchronous listener: billing computes the stay and locks the lines.
        // An event (not a direct call) keeps admission independent of billing (no cyclic dependency).
        applicationEvents.publishEvent(new AdmissionDischargedEvent(admissionId, at));
        log.info("Discharged admission {} after {} day(s)", admission.getAdmissionNo(),
                admission.lengthOfStayDays(at));
        return toResponse(admission);
    }

    @Override
    @Transactional
    public AdmissionResponse changeAttendingDoctor(Long admissionId, ChangeDoctorRequest request) {
        Admission admission = requireAdmission(admissionId);
        admission.changeAttendingDoctor(providerService.requireActiveDoctor(request.doctorId()));
        applicationEvents.publishEvent(new ChargeSourceChangedEvent(admissionId, "Attending doctor changed"));
        return toResponse(admission);
    }

    @Override
    @Transactional
    public AdmissionResponse cancel(Long admissionId) {
        Admission admission = requireAdmission(admissionId);
        admission.cancel();
        bedAllocationOperations.releaseBed(admissionId, LocalDateTime.now(clock));
        return toResponse(admission);
    }

    private AdmissionResponse toResponse(Admission admission) {
        List<BedAllocationResponse> history = bedAllocationOperations.allocationHistory(admission.getId());
        BedAllocationResponse current = history.stream().filter(b -> b.toDate() == null).findFirst().orElse(null);
        return new AdmissionResponse(AdmissionSummary.from(admission), admission.getDiagnosis(),
                admission.getDoctor().getDepartment().getDepartmentName(),
                admission.getPatient().getMedicareNo(), current, history, admission.getDoctor().getProviderNo());
    }
}
