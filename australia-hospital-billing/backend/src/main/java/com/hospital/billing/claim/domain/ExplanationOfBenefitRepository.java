package com.hospital.billing.claim.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExplanationOfBenefitRepository extends JpaRepository<ExplanationOfBenefit, Long> {

    Optional<ExplanationOfBenefit> findByClaimId(Long claimId);

    boolean existsByClaimId(Long claimId);
}
