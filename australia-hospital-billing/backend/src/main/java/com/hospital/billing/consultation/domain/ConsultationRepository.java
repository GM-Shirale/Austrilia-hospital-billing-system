package com.hospital.billing.consultation.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    @EntityGraph(attributePaths = {"doctor", "doctor.department"})
    List<Consultation> findByAdmissionIdOrderByConsultationAtAsc(Long admissionId);

    @EntityGraph(attributePaths = "doctor")
    List<Consultation> findByAdmissionIdAndStatusOrderByConsultationAtAsc(Long admissionId, ConsultationStatus status);
}
