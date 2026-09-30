package com.hospital.billing.lab.web;

import com.hospital.billing.common.web.PageResponse;
import com.hospital.billing.lab.domain.LabOrderStatus;
import com.hospital.billing.lab.service.LabService;
import com.hospital.billing.lab.web.LabDtos.LabOrderRequest;
import com.hospital.billing.lab.web.LabDtos.LabOrderResponse;
import com.hospital.billing.lab.web.LabDtos.LabTestRequest;
import com.hospital.billing.lab.web.LabDtos.LabTestResponse;
import com.hospital.billing.lab.web.LabDtos.ResultRequest;
import com.hospital.billing.lab.web.LabDtos.StatusUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Laboratory")
@RestController
@RequestMapping("/api/v1/lab")
@RequiredArgsConstructor
public class LabController {

    private final LabService labService;

    @Operation(summary = "Test catalogue; activeOnly=true for order forms")
    @GetMapping("/tests")
    public List<LabTestResponse> catalogue(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return labService.catalogue(activeOnly);
    }

    @PutMapping("/tests/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LAB')")
    public LabTestResponse updateTest(@PathVariable Long id, @Valid @RequestBody LabTestRequest request) {
        return labService.updateTest(id, request);
    }

    @Operation(summary = "Lab work queue: ORDERED and COLLECTED orders, oldest first")
    @GetMapping("/queue")
    public List<LabOrderResponse> queue() {
        return labService.queue();
    }

    @PostMapping("/tests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','LAB')")
    public LabTestResponse createTest(@Valid @RequestBody LabTestRequest request) {
        return labService.createTest(request);
    }

    @GetMapping("/orders")
    public PageResponse<LabOrderResponse> orders(
            @RequestParam(required = false) LabOrderStatus status,
            @PageableDefault(size = 20, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return labService.orders(status, pageable);
    }

    @GetMapping("/orders/by-admission/{admissionId}")
    public List<LabOrderResponse> ordersForAdmission(@PathVariable Long admissionId) {
        return labService.ordersForAdmission(admissionId);
    }

    @GetMapping("/orders/{id}")
    public LabOrderResponse order(@PathVariable Long id) {
        return labService.order(id);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','LAB')")
    public LabOrderResponse createOrder(@Valid @RequestBody LabOrderRequest request) {
        return labService.createOrder(request);
    }

    @PatchMapping("/orders/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','LAB','DOCTOR')")
    public LabOrderResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return labService.changeStatus(id, request.status(), request.notes());
    }

    @PutMapping("/orders/{id}/items/{itemId}/result")
    @PreAuthorize("hasAnyRole('ADMIN','LAB','DOCTOR')")
    public LabOrderResponse recordResult(@PathVariable Long id, @PathVariable Long itemId,
                                         @Valid @RequestBody ResultRequest request) {
        return labService.recordResult(id, itemId, request);
    }
}
