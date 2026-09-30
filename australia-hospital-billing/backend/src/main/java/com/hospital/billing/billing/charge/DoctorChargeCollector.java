package com.hospital.billing.billing.charge;

import com.hospital.billing.admission.domain.Admission;
import com.hospital.billing.admission.domain.CareSetting;
import com.hospital.billing.billing.domain.BillItemType;
import com.hospital.billing.consultation.domain.Consultation;
import com.hospital.billing.consultation.service.ConsultationService;
import com.hospital.billing.provider.domain.Doctor;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Professional fees, each billed against an MBS item:
 * <ol>
 *   <li>the attending doctor's admission attendance (or the OPD consultation);</li>
 *   <li>every COMPLETED consultation recorded during the admission.</li>
 * </ol>
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class DoctorChargeCollector implements ChargeCollector {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ConsultationService consultationService;

    @Override
    public List<ChargeLine> collect(Admission admission, LocalDateTime asOf) {
        List<ChargeLine> lines = new ArrayList<>();
        Doctor doctor = admission.getDoctor();
        String kind = admission.getCareSetting() == CareSetting.OPD ? "Out-patient consultation" : "Admission attendance";
        lines.add(new ChargeLine(BillItemType.DOCTOR, doctor.getId(),
                "%s - %s, %s (MBS item %s)".formatted(kind, doctor.displayName(), doctor.getSpecialization(),
                        doctor.getMbsItemNo()),
                doctor.getMbsItemNo(), doctor.getMbsScheduleFee(), BigDecimal.ONE, doctor.getConsultationFee(), null));

        for (Consultation c : consultationService.billableConsultations(admission.getId())) {
            lines.add(new ChargeLine(BillItemType.DOCTOR, c.getId(),
                    "Consultation %s - %s, %s %s (MBS item %s)".formatted(c.getConsultationNo(),
                            c.getDoctor().displayName(), c.getConsultationType().name().replace('_', ' ').toLowerCase(),
                            DATE.format(c.getConsultationAt()), c.getMbsItemNo()),
                    c.getMbsItemNo(), c.getMbsScheduleFee(), BigDecimal.ONE, c.getFee(), null));
        }
        return lines;
    }
}
