package com.hospital.billing.claim.domain;

import com.hospital.billing.insurance.domain.PayerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InsuranceClaimRepository extends JpaRepository<InsuranceClaim, Long> {

    @Override
    @EntityGraph(attributePaths = {"company", "bill", "admission", "admission.patient"})
    Page<InsuranceClaim> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"company", "bill", "admission", "admission.patient"})
    Page<InsuranceClaim> findByStatus(ClaimStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"company", "bill", "admission", "admission.patient"})
    List<InsuranceClaim> findByBillIdOrderByIdAsc(Long billId);

    @EntityGraph(attributePaths = {"company", "policy", "bill", "admission", "admission.patient", "admission.doctor",
            "items", "items.billItem"})
    Optional<InsuranceClaim> findWithItemsById(Long id);

    boolean existsByBillIdAndPayerType(Long billId, PayerType payerType);

    @Query("select c.status, count(c) from InsuranceClaim c group by c.status")
    List<Object[]> countGroupedByStatus();
}
