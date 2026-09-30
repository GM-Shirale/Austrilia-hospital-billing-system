package com.hospital.billing.lab.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LabOrderRepository extends JpaRepository<LabOrder, Long> {

    @Override
    @EntityGraph(attributePaths = {"admission", "admission.patient", "doctor"})
    Page<LabOrder> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"admission", "admission.patient", "doctor"})
    Page<LabOrder> findByStatus(LabOrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"admission", "admission.patient", "doctor", "items", "items.labTest"})
    Optional<LabOrder> findWithItemsById(Long id);

    @EntityGraph(attributePaths = {"doctor", "items", "items.labTest"})
    List<LabOrder> findByAdmissionIdAndStatusNot(Long admissionId, LabOrderStatus status);

    @EntityGraph(attributePaths = {"admission", "admission.patient", "doctor", "items", "items.labTest"})
    List<LabOrder> findByAdmissionIdOrderByOrderDateDesc(Long admissionId);

    /** Work queue: oldest first so the lab processes orders in arrival order. */
    @EntityGraph(attributePaths = {"admission", "admission.patient", "doctor", "items", "items.labTest"})
    List<LabOrder> findByStatusInOrderByOrderDateAsc(List<LabOrderStatus> statuses);

    long countByStatusIn(List<LabOrderStatus> statuses);
}
