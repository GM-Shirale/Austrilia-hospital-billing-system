package com.hospital.billing.patient.service;

import com.hospital.billing.common.web.PageResponse;
import com.hospital.billing.patient.domain.Patient;
import com.hospital.billing.patient.web.PatientDtos.ContactRequest;
import com.hospital.billing.patient.web.PatientDtos.PatientRequest;
import com.hospital.billing.patient.web.PatientDtos.PatientResponse;
import com.hospital.billing.patient.web.PatientDtos.PatientSummary;
import org.springframework.data.domain.Pageable;

public interface PatientService {

    PageResponse<PatientSummary> search(String term, Pageable pageable);

    PatientResponse get(Long id);

    PatientResponse register(PatientRequest request);

    PatientResponse update(Long id, PatientRequest request);

    PatientResponse addContact(Long patientId, ContactRequest request);

    PatientResponse removeContact(Long patientId, Long contactId);

    /** For other modules: loads the entity or throws EntityNotFoundException. */
    Patient requirePatient(Long id);
}
