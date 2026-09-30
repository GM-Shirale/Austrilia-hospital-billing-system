package com.hospital.billing.adjudication;

import com.hospital.billing.adjudication.CoverageProfile.PrivateCover;
import com.hospital.billing.admission.domain.CareSetting;
import com.hospital.billing.admission.domain.FinancialClass;
import com.hospital.billing.billing.domain.BillItemType;
import com.hospital.billing.insurance.domain.CoverTier;
import com.hospital.billing.insurance.domain.NetworkStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure unit tests: no Spring context, no database; the engine is plain Java. */
class AdjudicationEngineTest {

    private final AdjudicationEngine engine = new AdjudicationEngine(
            new AdjudicatorResolver(List.of(new MedicareEclipseAdjudicator(), new PrivateFundAdjudicator())));

    private static final AdjudicationLine DOCTOR = new AdjudicationLine(1L, BillItemType.DOCTOR, bd("320.00"), bd("170.50"));
    private static final AdjudicationLine ROOM = new AdjudicationLine(2L, BillItemType.ROOM, bd("1350.00"), null);
    private static final AdjudicationLine PHARMACY = new AdjudicationLine(3L, BillItemType.PHARMACY, bd("100.00"), null);

    @Test
    @DisplayName("Private patient, GOLD no-gap fund: Medicare 75% of schedule fee, fund pays the rest, patient pays only the excess")
    void goldNoGapCover() {
        CoverageProfile coverage = privatePatient(CoverTier.GOLD, NetworkStatus.NO_GAP, null, "250.00", "100000.00");

        AdjudicationResult result = engine.adjudicate(List.of(DOCTOR, ROOM, PHARMACY), coverage);
        Map<Long, LineOutcome> lines = result.byBillItemId();

        // 75% of 170.50 = 127.875 -> 127.88
        assertEquals(bd("127.88"), lines.get(1L).medicare());
        // fund covers everything that is left on the doctor line (no-gap agreement)
        assertEquals(bd("192.12"), lines.get(1L).privateFund());
        assertEquals(bd("0.00"), lines.get(1L).patientGap());
        // accommodation: 100% cover minus the $250 excess
        assertEquals(bd("1100.00"), lines.get(2L).privateFund());
        assertEquals(bd("250.00"), lines.get(2L).patientGap());
        // GOLD pharmacy cover is 100%
        assertEquals(bd("100.00"), lines.get(3L).privateFund());
        assertEquals(bd("250.00"), result.totalPatientGap());
        assertBalanced(result, "1770.00");
    }

    @Test
    @DisplayName("Known-gap agreement: patient pays at most the known-gap cap above the schedule fee")
    void knownGapIsCapped() {
        AdjudicationLine surgeon = new AdjudicationLine(1L, BillItemType.SURGERY, bd("1200.00"), bd("170.50"));
        CoverageProfile coverage = privatePatient(CoverTier.SILVER_PLUS, NetworkStatus.KNOWN_GAP, "500.00", "0.00", "60000.00");

        LineOutcome outcome = engine.adjudicate(List.of(surgeon), coverage).byBillItemId().get(1L);

        assertEquals(bd("127.88"), outcome.medicare());
        assertTrue(outcome.patientGap().compareTo(bd("500.00")) <= 0, "gap must not exceed the known-gap cap");
        assertEquals(bd("1200.00"), outcome.medicare().add(outcome.privateFund()).add(outcome.patientGap()));
    }

    @Test
    @DisplayName("Non-participating fund: patient pays everything above the schedule fee")
    void nonParticipatingFund() {
        CoverageProfile coverage = privatePatient(CoverTier.BRONZE, NetworkStatus.NON_PARTICIPATING, null, "0.00", "30000.00");

        LineOutcome outcome = engine.adjudicate(List.of(DOCTOR), coverage).byBillItemId().get(1L);

        assertEquals(bd("127.88"), outcome.medicare());
        assertEquals(bd("42.63"), outcome.privateFund());   // 25% of 170.50
        assertEquals(bd("149.49"), outcome.patientGap());   // 320.00 - 127.88 - 42.63
    }

    @Test
    @DisplayName("Public patient: treatment fully funded, nothing for the patient to pay")
    void publicPatientPaysNothing() {
        CoverageProfile coverage = new CoverageProfile(FinancialClass.PUBLIC, true, null);

        AdjudicationResult result = engine.adjudicate(List.of(DOCTOR, ROOM), coverage);

        assertEquals(bd("1670.00"), result.totalMedicare());
        assertEquals(bd("0.00"), result.totalPatientGap());
    }

    @Test
    @DisplayName("Private out-patient (OPD): Medicare 85% of schedule fee, no fund benefit, 0% on accommodation")
    void outpatientGets85PercentAndNoFundBenefit() {
        PrivateCover cover = new PrivateCover(10L, 20L, "Test Fund", "POL-1", CoverTier.GOLD, NetworkStatus.NO_GAP,
                null, bd("0.00"), bd("100000.00"));
        CoverageProfile coverage = new CoverageProfile(FinancialClass.PRIVATE, CareSetting.OPD, true, cover);

        AdjudicationResult result = engine.adjudicate(List.of(DOCTOR, ROOM), coverage);
        Map<Long, LineOutcome> lines = result.byBillItemId();

        // 85% of 170.50 = 144.925 -> 144.93
        assertEquals(bd("144.93"), lines.get(1L).medicare());
        assertEquals(bd("175.07"), lines.get(1L).patientGap());
        assertEquals(bd("0.00"), lines.get(2L).medicare());
        assertEquals(bd("0.00"), result.totalPrivateFund());
        assertBalanced(result, "1670.00");
    }

    @Test
    @DisplayName("Public patient: 100% regardless of care setting")
    void publicOutpatientIsFullyFunded() {
        CoverageProfile coverage = new CoverageProfile(FinancialClass.PUBLIC, CareSetting.OPD, true, null);

        AdjudicationResult result = engine.adjudicate(List.of(DOCTOR), coverage);

        assertEquals(bd("320.00"), result.totalMedicare());
        assertEquals(bd("0.00"), result.totalPatientGap());
    }

    @Test
    @DisplayName("No Medicare card and no private cover: the patient pays the full amount")
    void uninsuredPatientPaysAll() {
        CoverageProfile coverage = new CoverageProfile(FinancialClass.PRIVATE, false, null);

        AdjudicationResult result = engine.adjudicate(List.of(DOCTOR, ROOM), coverage);

        assertEquals(bd("0.00"), result.totalMedicare());
        assertEquals(bd("0.00"), result.totalPrivateFund());
        assertEquals(bd("1670.00"), result.totalPatientGap());
    }

    @Test
    @DisplayName("Benefit accumulator: fund never pays more than the remaining annual limit")
    void annualLimitCapsFundBenefit() {
        CoverageProfile coverage = privatePatient(CoverTier.GOLD, NetworkStatus.NO_GAP, null, "0.00", "500.00");

        AdjudicationResult result = engine.adjudicate(List.of(DOCTOR, ROOM), coverage);

        assertEquals(bd("500.00"), result.totalPrivateFund());
        assertBalanced(result, "1670.00");
    }

    private static CoverageProfile privatePatient(CoverTier tier, NetworkStatus network, String knownGapCap,
                                                  String excess, String remainingLimit) {
        PrivateCover cover = new PrivateCover(10L, 20L, "Test Fund", "POL-1", tier, network,
                knownGapCap == null ? null : bd(knownGapCap), bd(excess), bd(remainingLimit));
        return new CoverageProfile(FinancialClass.PRIVATE, true, cover);
    }

    private static void assertBalanced(AdjudicationResult result, String expectedNet) {
        BigDecimal total = result.totalMedicare().add(result.totalPrivateFund()).add(result.totalPatientGap());
        assertEquals(bd(expectedNet), total, "medicare + fund + patient must equal the net amount");
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
