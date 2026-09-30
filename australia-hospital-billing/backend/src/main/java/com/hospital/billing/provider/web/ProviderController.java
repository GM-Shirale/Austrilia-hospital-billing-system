package com.hospital.billing.provider.web;

import com.hospital.billing.common.web.PageResponse;
import com.hospital.billing.provider.service.ProviderService;
import com.hospital.billing.provider.web.ProviderDtos.DepartmentRequest;
import com.hospital.billing.provider.web.ProviderDtos.DepartmentResponse;
import com.hospital.billing.provider.web.ProviderDtos.DoctorRequest;
import com.hospital.billing.provider.web.ProviderDtos.DoctorResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Departments & Doctors")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderService providerService;

    @GetMapping("/departments")
    public List<DepartmentResponse> departments() {
        return providerService.listDepartments();
    }

    @PostMapping("/departments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponse createDepartment(@Valid @RequestBody DepartmentRequest request) {
        return providerService.createDepartment(request);
    }

    @GetMapping("/doctors")
    public PageResponse<DoctorResponse> doctors(
            @RequestParam(required = false) Long departmentId,
            @PageableDefault(size = 20, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable) {
        return providerService.listDoctors(departmentId, pageable);
    }

    @GetMapping("/doctors/{id}")
    public DoctorResponse doctor(@PathVariable Long id) {
        return providerService.getDoctor(id);
    }

    @PostMapping("/doctors")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public DoctorResponse createDoctor(@Valid @RequestBody DoctorRequest request) {
        return providerService.createDoctor(request);
    }

    @PutMapping("/doctors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DoctorResponse updateDoctor(@PathVariable Long id, @Valid @RequestBody DoctorRequest request) {
        return providerService.updateDoctor(id, request);
    }
}
