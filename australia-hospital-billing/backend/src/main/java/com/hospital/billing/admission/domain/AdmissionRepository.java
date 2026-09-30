package com.hospital.billing.admission.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdmissionRepository extends JpaRepository<Admission, Long> {

    @Override
    @EntityGraph(attributePaths = {"patient", "doctor", "admissionType"})
    Page<Admission> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"patient", "doctor", "admissionType"})
    Page<Admission> findByStatus(AdmissionStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"patient", "doctor", "admissionType"})
    List<Admission> findByPatientIdOrderByAdmissionDateDesc(Long patientId);

    @EntityGraph(attributePaths = {"patient", "doctor", "doctor.department", "admissionType"})
    Optional<Admission> findWithDetailsById(Long id);

    boolean existsByPatientIdAndStatus(Long patientId, AdmissionStatus status);

    long countByStatus(AdmissionStatus status);
}
