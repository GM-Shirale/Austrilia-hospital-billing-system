package com.hospital.billing.pharmacy.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    @Override
    @EntityGraph(attributePaths = {"admission", "admission.patient", "doctor"})
    Page<Prescription> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"admission", "admission.patient", "doctor"})
    Page<Prescription> findByStatus(PrescriptionStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"admission", "admission.patient", "doctor", "items", "items.medicine"})
    Optional<Prescription> findWithItemsById(Long id);

    @EntityGraph(attributePaths = {"items", "items.medicine"})
    List<Prescription> findByAdmissionIdAndStatus(Long admissionId, PrescriptionStatus status);

    @EntityGraph(attributePaths = {"admission", "admission.patient", "doctor", "items", "items.medicine"})
    List<Prescription> findByAdmissionIdOrderByPrescriptionDateDesc(Long admissionId);

    /** Pharmacy queue: prescriptions awaiting dispensing, oldest first. */
    @EntityGraph(attributePaths = {"admission", "admission.patient", "doctor", "items", "items.medicine"})
    List<Prescription> findByStatusOrderByPrescriptionDateAsc(PrescriptionStatus status);

    long countByAdmissionIdAndStatus(Long admissionId, PrescriptionStatus status);
}
