package com.hospital.billing.billing.web;

import com.hospital.billing.billing.service.InvoiceService;
import com.hospital.billing.billing.web.InvoiceDtos.InvoiceResponse;
import com.hospital.billing.billing.ai.AiBillingDtos.BillExplanationResponse;
import com.hospital.billing.billing.ai.BillExplanationService;
import com.hospital.billing.billing.domain.BillStatus;
import com.hospital.billing.billing.service.BillingService;
import com.hospital.billing.billing.service.PaymentService;
import com.hospital.billing.billing.web.BillingDtos.BillResponse;
import com.hospital.billing.billing.web.BillingDtos.BillSummary;
import com.hospital.billing.billing.web.BillingDtos.GenerateBillRequest;
import com.hospital.billing.billing.web.BillingDtos.ManualItemRequest;
import com.hospital.billing.billing.web.BillingDtos.PaymentRequest;
import com.hospital.billing.billing.web.BillingDtos.PaymentResponse;
import com.hospital.billing.billing.web.BillingDtos.RefundRequest;
import com.hospital.billing.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Billing API. Every endpoint is restricted to ADMIN or BILLING staff; the tenant filter
 * guarantees they only ever see their own hospital's bills.
 */
@Tag(name = "Billing & Payments")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('BILLING_OFFICER')")
public class BillingController {

    private final BillingService billingService;
    private final PaymentService paymentService;
    private final BillExplanationService billExplanationService;
    private final InvoiceService invoiceService;

    @Operation(summary = "List bills (paged, filter by status or admission)")
    @GetMapping("/bills")
    public PageResponse<BillSummary> search(
            @RequestParam(required = false) BillStatus status,
            @RequestParam(required = false) Long admissionId,
            @PageableDefault(size = 20, sort = "billDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return billingService.search(status, admissionId, pageable);
    }

    @GetMapping("/bills/{id}")
    public BillResponse get(@PathVariable Long id) {
        return billingService.get(id);
    }

    @Operation(summary = "Tax invoice / discharge summary data (hospital letterhead, patient, stay, lines, payments)")
    @GetMapping("/bills/{id}/invoice")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_OFFICER','RECEPTIONIST','DOCTOR')")
    public InvoiceResponse invoice(@PathVariable Long id) {
        return invoiceService.invoice(id);
    }

    @Operation(summary = "Bills of an admission (any role that can see the admission)")
    @GetMapping("/admissions/{admissionId}/bills")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_OFFICER','RECEPTIONIST','DOCTOR')")
    public PageResponse<BillSummary> billsOfAdmission(@PathVariable Long admissionId,
                                                     @PageableDefault(size = 10) Pageable pageable) {
        return billingService.search(null, admissionId, pageable);
    }

    @Operation(summary = "Generate a DRAFT bill from all charges of an admission, with Medicare/fund estimate")
    @PostMapping("/bills")
    @ResponseStatus(HttpStatus.CREATED)
    public BillResponse generate(@Valid @RequestBody GenerateBillRequest request) {
        return billingService.generateBill(request.admissionId());
    }

    @PostMapping("/bills/{id}/recalculate")
    public BillResponse recalculate(@PathVariable Long id) {
        return billingService.recalculate(id);
    }

    @Operation(summary = "Add a manual line (surgery, procedure, nursing, equipment, other)")
    @PostMapping("/bills/{id}/items")
    public BillResponse addItem(@PathVariable Long id, @Valid @RequestBody ManualItemRequest request) {
        return billingService.addManualItem(id, request);
    }

    @DeleteMapping("/bills/{id}/items/{itemId}")
    public BillResponse removeItem(@PathVariable Long id, @PathVariable Long itemId) {
        return billingService.removeItem(id, itemId);
    }

    @PostMapping("/bills/{id}/finalize")
    public BillResponse finalizeBill(@PathVariable Long id) {
        return billingService.finalizeBill(id);
    }

    @PostMapping("/bills/{id}/cancel")
    public BillResponse cancel(@PathVariable Long id) {
        return billingService.cancel(id);
    }

    @Operation(summary = "Plain-English explanation of the bill for the patient (Spring AI, template fallback)")
    @GetMapping("/bills/{id}/ai-explanation")
    public BillExplanationResponse explain(@PathVariable Long id) {
        return billExplanationService.explain(id);
    }

    @GetMapping("/bills/{id}/payments")
    public List<PaymentResponse> payments(@PathVariable Long id) {
        return paymentService.paymentsForBill(id);
    }

    @Operation(summary = "Record a patient payment (limited to the patient's share)")
    @PostMapping("/bills/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse pay(@PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
        return paymentService.recordPatientPayment(id, request);
    }

    @PostMapping("/payments/{paymentId}/refunds")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse refund(@PathVariable Long paymentId, @Valid @RequestBody RefundRequest request) {
        return paymentService.refund(paymentId, request);
    }
}
