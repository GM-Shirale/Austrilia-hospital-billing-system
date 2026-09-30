package com.hospital.billing.admission.service;

import com.hospital.billing.admission.domain.Admission;
import com.hospital.billing.admission.domain.AdmissionStatus;
import com.hospital.billing.admission.web.AdmissionDtos.AdmissionResponse;
import com.hospital.billing.admission.web.AdmissionDtos.AdmissionSummary;
import com.hospital.billing.admission.web.AdmissionDtos.AdmissionTypeResponse;
import com.hospital.billing.common.web.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Interface Segregation: read-only admission queries. Billing, lab and pharmacy depend on
 * this contract only, so they cannot (even accidentally) discharge a patient.
 */
public interface AdmissionReadOperations {

    PageResponse<AdmissionSummary> search(AdmissionStatus status, Pageable pageable);

    AdmissionResponse get(Long admissionId);

    List<AdmissionSummary> findByPatient(Long patientId);

    List<AdmissionTypeResponse> admissionTypes();

    /** Entity access for other modules inside the monolith. */
    Admission requireAdmission(Long admissionId);
}
