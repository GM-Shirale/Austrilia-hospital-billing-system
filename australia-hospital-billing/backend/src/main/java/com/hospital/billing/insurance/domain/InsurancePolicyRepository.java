package com.hospital.billing.insurance.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, Long> {

    @EntityGraph(attributePaths = "company")
    List<InsurancePolicy> findByPatientIdOrderByStartDateDesc(Long patientId);

    @EntityGraph(attributePaths = {"company", "patient"})
    Optional<InsurancePolicy> findWithCompanyById(Long id);

    boolean existsByCompanyIdAndPolicyNoIgnoreCase(Long companyId, String policyNo);
}
