package com.hospital.billing.insurance.domain;

/**
 * The hospital's contract with a fund, which decides how charges ABOVE the MBS schedule
 * fee are split between the fund and the patient.
 */
public enum NetworkStatus {
    /** Fund pays the full charge; no out-of-pocket gap for medical items. */
    NO_GAP,
    /** Fund pays the charge above the schedule fee except a capped "known gap" per item. */
    KNOWN_GAP,
    /** Fund only pays the 25% top-up of the schedule fee; the patient pays everything above. */
    NON_PARTICIPATING,
    /** Not a private fund (Medicare). */
    NOT_APPLICABLE
}
