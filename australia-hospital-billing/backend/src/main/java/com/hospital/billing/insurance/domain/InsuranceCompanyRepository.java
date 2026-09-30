package com.hospital.billing.insurance.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsuranceCompanyRepository extends JpaRepository<InsuranceCompany, Long> {

    List<InsuranceCompany> findAllByOrderByCompanyNameAsc();

    Optional<InsuranceCompany> findFirstByPayerType(PayerType payerType);

    boolean existsByCompanyNameIgnoreCase(String companyName);
}
