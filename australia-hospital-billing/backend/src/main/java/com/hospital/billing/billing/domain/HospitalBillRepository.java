package com.hospital.billing.billing.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

public interface HospitalBillRepository extends JpaRepository<HospitalBill, Long> {

    @Override
    @EntityGraph(attributePaths = {"admission", "admission.patient"})
    Page<HospitalBill> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"admission", "admission.patient"})
    Page<HospitalBill> findByStatus(BillStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"admission", "admission.patient"})
    Page<HospitalBill> findByAdmissionId(Long admissionId, Pageable pageable);

    @EntityGraph(attributePaths = {"admission", "admission.patient", "admission.doctor", "items"})
    Optional<HospitalBill> findWithItemsById(Long id);

    /** The live (not cancelled) bill of an admission, if one was generated. */
    Optional<HospitalBill> findFirstByAdmissionIdAndStatusNotOrderByIdDesc(Long admissionId, BillStatus status);

    boolean existsByAdmissionIdAndStatusNot(Long admissionId, BillStatus status);

    long countByStatus(BillStatus status);

    /** Null when there are no matching bills; callers wrap it with Money.of(). */
    @Query("select sum(b.netAmount - b.paidAmount) from HospitalBill b where b.status in :statuses")
    BigDecimal sumOutstanding(@Param("statuses") Collection<BillStatus> statuses);
}
