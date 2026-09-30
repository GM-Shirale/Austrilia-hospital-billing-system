package com.hospital.billing.provider.domain;

import com.hospital.billing.common.domain.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "department")
public class Department extends BaseTenantEntity {

    @Column(name = "department_name", nullable = false, length = 100)
    private String departmentName;

    @Column(length = 100)
    private String location;
}
