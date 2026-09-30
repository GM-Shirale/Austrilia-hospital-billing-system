package com.hospital.billing.admission.domain;

/**
 * Where the care is delivered. Drives the Medicare rebate rate for private patients:
 * <ul>
 *   <li>{@link #IPD} in-patient (admitted, occupies a bed): 75% of the MBS schedule fee;</li>
 *   <li>{@link #OPD} out-patient (clinic visit, no bed): 85% of the MBS schedule fee.</li>
 * </ul>
 */
public enum CareSetting {
    IPD,
    OPD;

    public boolean requiresBed() {
        return this == IPD;
    }
}
