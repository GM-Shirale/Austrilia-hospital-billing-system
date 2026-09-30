package com.hospital.billing.lab.service;

import com.hospital.billing.common.event.ChargeSourceChangedEvent;
import com.hospital.billing.common.exception.BusinessRuleException;
import com.hospital.billing.security.CurrentUser;
import org.springframework.context.ApplicationEventPublisher;
import com.hospital.billing.admission.domain.Admission;
import com.hospital.billing.admission.service.AdmissionReadOperations;
import com.hospital.billing.common.domain.DocumentNumberGenerator;
import com.hospital.billing.common.domain.DocumentType;
import com.hospital.billing.common.exception.DuplicateResourceException;
import com.hospital.billing.common.exception.EntityNotFoundException;
import com.hospital.billing.common.money.Money;
import com.hospital.billing.common.web.PageResponse;
import com.hospital.billing.lab.domain.LabOrder;
import com.hospital.billing.lab.domain.LabOrderItem;
import com.hospital.billing.lab.domain.LabOrderRepository;
import com.hospital.billing.lab.domain.LabOrderStatus;
import com.hospital.billing.lab.domain.LabTest;
import com.hospital.billing.lab.domain.LabTestRepository;
import com.hospital.billing.lab.web.LabDtos.LabOrderRequest;
import com.hospital.billing.lab.web.LabDtos.LabOrderResponse;
import com.hospital.billing.lab.web.LabDtos.LabTestRequest;
import com.hospital.billing.lab.web.LabDtos.LabTestResponse;
import com.hospital.billing.lab.web.LabDtos.ResultRequest;
import com.hospital.billing.provider.service.ProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabService {

    private final LabTestRepository labTestRepository;
    private final LabOrderRepository labOrderRepository;
    private final AdmissionReadOperations admissionReads;
    private final ProviderService providerService;
    private final DocumentNumberGenerator documentNumberGenerator;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    /** Full catalogue (catalogue tab), or only active tests (order forms). */
    public List<LabTestResponse> catalogue(boolean activeOnly) {
        List<LabTest> tests = activeOnly
                ? labTestRepository.findByActiveTrueOrderByCategoryAscTestNameAsc()
                : labTestRepository.findAllByOrderByCategoryAscTestNameAsc();
        return tests.stream().map(LabTestResponse::from).toList();
    }

    @Transactional
    public LabTestResponse createTest(LabTestRequest request) {
        String code = request.testCode().trim().toUpperCase();
        if (labTestRepository.existsByTestCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Lab test code %s already exists".formatted(code));
        }
        LabTest test = new LabTest();
        test.setTestCode(code);
        apply(test, request);
        return LabTestResponse.from(labTestRepository.save(test));
    }

    /** Price changes apply to new orders only: ordered items keep the price copied at ordering. */
    @Transactional
    public LabTestResponse updateTest(Long id, LabTestRequest request) {
        LabTest test = labTestRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Lab test", id));
        apply(test, request);
        return LabTestResponse.from(test);
    }

    private static void apply(LabTest test, LabTestRequest request) {
        String mbs = request.mbsItemNo() == null || request.mbsItemNo().isBlank() ? null : request.mbsItemNo().trim();
        boolean gstFree = request.gstFree() == null || request.gstFree();
        if (mbs != null && !gstFree) {
            throw new BusinessRuleException("Tests billed under an MBS item are GST-free medical services");
        }
        if (mbs != null && request.mbsScheduleFee() == null) {
            throw new BusinessRuleException("Enter the MBS schedule fee for item " + mbs);
        }
        test.setTestName(request.testName().trim());
        test.setCategory(request.category().trim());
        test.setTestType(request.testType());
        test.setUnitPrice(Money.of(request.unitPrice()));
        test.setMbsItemNo(mbs);
        test.setMbsScheduleFee(mbs == null ? null : Money.of(request.mbsScheduleFee()));
        test.setGstFree(gstFree);
        test.setActive(request.active() == null || request.active());
    }

    public PageResponse<LabOrderResponse> orders(LabOrderStatus status, Pageable pageable) {
        Page<LabOrder> page = status == null
                ? labOrderRepository.findAll(pageable)
                : labOrderRepository.findByStatus(status, pageable);
        return PageResponse.from(page.map(LabOrderResponse::summary));
    }

    public LabOrderResponse order(Long id) {
        return LabOrderResponse.detail(requireOrder(id));
    }

    @Transactional
    public LabOrderResponse createOrder(LabOrderRequest request) {
        Admission admission = admissionReads.requireAdmission(request.admissionId());
        admission.requireActive();

        LabOrder order = new LabOrder();
        order.setOrderNo(documentNumberGenerator.next(DocumentType.LAB_ORDER));
        order.setAdmission(admission);
        order.setDoctor(providerService.requireActiveDoctor(request.doctorId()));
        order.setOrderDate(LocalDateTime.now(clock));
        order.setOrderedByUserId(CurrentUser.userIdOrNull());
        order.setOrderedByName(CurrentUser.displayName());
        request.items().forEach(line -> {
            LabTest test = labTestRepository.findById(line.labTestId())
                    .orElseThrow(() -> new EntityNotFoundException("Lab test", line.labTestId()));
            if (!test.isActive()) {
                throw new BusinessRuleException("%s is no longer offered".formatted(test.getTestName()));
            }
            LabOrderItem item = new LabOrderItem();
            item.setLabTest(test);
            item.setQuantity(line.quantity());
            item.setPrice(test.getUnitPrice());
            order.addItem(item);
        });
        LabOrder saved = labOrderRepository.save(order);
        events.publishEvent(new ChargeSourceChangedEvent(admission.getId(), "Lab order " + saved.getOrderNo()));
        return LabOrderResponse.detail(saved);
    }

    /** Lab queue: ORDERED and COLLECTED orders, oldest first. */
    public List<LabOrderResponse> queue() {
        return labOrderRepository.findByStatusInOrderByOrderDateAsc(
                        List.of(LabOrderStatus.ORDERED, LabOrderStatus.COLLECTED)).stream()
                .map(LabOrderResponse::detail)
                .toList();
    }

    @Transactional
    public LabOrderResponse changeStatus(Long id, LabOrderStatus status, String notes) {
        LabOrder order = requireOrder(id);
        if (status == LabOrderStatus.CANCELLED) {
            order.getAdmission().requireActive();   // charges are locked once the patient is discharged
        }
        if (status == LabOrderStatus.COMPLETED && order.getItems().stream().anyMatch(i -> i.getResult() == null)
                && (notes == null || notes.isBlank())) {
            throw new BusinessRuleException("Record a result for every test, or add result notes, before completing");
        }
        order.transition(status, CurrentUser.userIdOrNull(), CurrentUser.displayName(), LocalDateTime.now(clock), notes);
        if (status == LabOrderStatus.CANCELLED) {
            events.publishEvent(new ChargeSourceChangedEvent(order.getAdmission().getId(),
                    "Lab order " + order.getOrderNo() + " cancelled"));
        }
        return LabOrderResponse.detail(order);
    }

    @Transactional
    public LabOrderResponse recordResult(Long orderId, Long itemId, ResultRequest request) {
        LabOrder order = requireOrder(orderId);
        LabOrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Lab order item", itemId));
        if (order.getStatus() == LabOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Order %s is cancelled".formatted(order.getOrderNo()));
        }
        item.recordResult(request.result().trim(), CurrentUser.displayName(), LocalDateTime.now(clock));
        return LabOrderResponse.detail(order);
    }

    public List<LabOrderResponse> ordersForAdmission(Long admissionId) {
        return labOrderRepository.findByAdmissionIdOrderByOrderDateDesc(admissionId).stream()
                .map(LabOrderResponse::detail)
                .toList();
    }

    /** For billing: every order that was not cancelled is chargeable. */
    public List<LabOrder> chargeableOrders(Long admissionId) {
        return labOrderRepository.findByAdmissionIdAndStatusNot(admissionId, LabOrderStatus.CANCELLED);
    }

    private LabOrder requireOrder(Long id) {
        return labOrderRepository.findWithItemsById(id).orElseThrow(() -> new EntityNotFoundException("Lab order", id));
    }
}
