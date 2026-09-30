package com.hospital.billing.provider.service;

import com.hospital.billing.common.exception.BusinessRuleException;
import com.hospital.billing.common.exception.DuplicateResourceException;
import com.hospital.billing.common.exception.EntityNotFoundException;
import com.hospital.billing.common.money.Money;
import com.hospital.billing.common.web.PageResponse;
import com.hospital.billing.provider.domain.Department;
import com.hospital.billing.provider.domain.DepartmentRepository;
import com.hospital.billing.provider.domain.Doctor;
import com.hospital.billing.provider.domain.DoctorRepository;
import com.hospital.billing.provider.web.ProviderDtos.DepartmentRequest;
import com.hospital.billing.provider.web.ProviderDtos.DepartmentResponse;
import com.hospital.billing.provider.web.ProviderDtos.DoctorRequest;
import com.hospital.billing.provider.web.ProviderDtos.DoctorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Departments and doctors (providers).
 *
 * <p>Cache keys include the tenant id: a cache shared by all hospitals keyed only by
 * method name would leak one hospital's data to another, a classic multi-tenancy bug.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProviderService {

    static final String TENANT_KEY = "T(com.hospital.billing.tenant.TenantContext).requireCurrentTenant()";

    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;

    @Cacheable(cacheNames = "departments", key = TENANT_KEY)
    public List<DepartmentResponse> listDepartments() {
        return departmentRepository.findAllByOrderByDepartmentNameAsc().stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = "departments", key = TENANT_KEY)
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByDepartmentNameIgnoreCase(request.departmentName().trim())) {
            throw new DuplicateResourceException("Department '%s' already exists".formatted(request.departmentName()));
        }
        Department department = new Department();
        department.setDepartmentName(request.departmentName().trim());
        department.setLocation(request.location());
        return DepartmentResponse.from(departmentRepository.save(department));
    }

    public PageResponse<DoctorResponse> listDoctors(Long departmentId, Pageable pageable) {
        var page = departmentId == null
                ? doctorRepository.findAll(pageable)
                : doctorRepository.findByDepartmentId(departmentId, pageable);
        return PageResponse.from(page.map(DoctorResponse::from));
    }

    public DoctorResponse getDoctor(Long id) {
        return DoctorResponse.from(requireDoctor(id));
    }

    @Transactional
    public DoctorResponse createDoctor(DoctorRequest request) {
        if (doctorRepository.existsByProviderNoIgnoreCase(request.providerNo())) {
            throw new DuplicateResourceException("Provider number %s is already registered".formatted(request.providerNo()));
        }
        Doctor doctor = new Doctor();
        apply(doctor, request);
        return DoctorResponse.from(doctorRepository.save(doctor));
    }

    @Transactional
    public DoctorResponse updateDoctor(Long id, DoctorRequest request) {
        Doctor doctor = requireDoctor(id);
        if (!doctor.getProviderNo().equalsIgnoreCase(request.providerNo())
                && doctorRepository.existsByProviderNoIgnoreCase(request.providerNo())) {
            throw new DuplicateResourceException("Provider number %s is already registered".formatted(request.providerNo()));
        }
        apply(doctor, request);
        return DoctorResponse.from(doctor);
    }

    /** Used by other modules (admission, lab, pharmacy). */
    public Doctor requireActiveDoctor(Long id) {
        Doctor doctor = requireDoctor(id);
        if (!doctor.isActive()) {
            throw new BusinessRuleException("%s is not active".formatted(doctor.displayName()));
        }
        return doctor;
    }

    private Doctor requireDoctor(Long id) {
        return doctorRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Doctor", id));
    }

    private void apply(Doctor doctor, DoctorRequest request) {
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department", request.departmentId()));
        doctor.setDepartment(department);
        doctor.setFirstName(request.firstName().trim());
        doctor.setLastName(request.lastName().trim());
        doctor.setSpecialization(request.specialization().trim());
        doctor.setPhone(request.phone());
        doctor.setEmail(request.email());
        doctor.setProviderNo(request.providerNo().toUpperCase());
        doctor.setConsultationFee(Money.of(request.consultationFee()));
        doctor.setMbsItemNo(request.mbsItemNo());
        doctor.setMbsScheduleFee(Money.of(request.mbsScheduleFee()));
        if (request.active() != null) {
            doctor.setActive(request.active());
        }
    }
}
