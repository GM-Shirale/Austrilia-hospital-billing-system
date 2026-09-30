-- =====================================================================
-- V2: Indexing strategy
--
-- 1. Composite tenant indexes: tenant_id is always the leading column because
--    Hibernate @TenantId adds "tenant_id = ?" to EVERY query.
-- 2. Foreign-key covering indexes: PostgreSQL does NOT index FK columns
--    automatically. Without them, a DELETE/UPDATE on the parent row does a
--    sequential scan of the child table while holding locks, and joins during
--    remittance reconciliation degrade to hash joins over whole tables.
-- 3. Temporal overlap index + exclusion constraint for bed allocation.
-- =====================================================================

-- ---- 1. Composite tenant indexes ------------------------------------
CREATE INDEX ix_patient_tenant_created      ON patient (tenant_id, created_at DESC);
CREATE INDEX ix_patient_tenant_medicare     ON patient (tenant_id, medicare_no);
CREATE INDEX ix_patient_tenant_name         ON patient (tenant_id, lower(last_name), lower(first_name));
CREATE INDEX ix_patient_tenant_status       ON patient (tenant_id, status);

CREATE INDEX ix_admission_tenant_status     ON admission (tenant_id, status);
CREATE INDEX ix_admission_tenant_created    ON admission (tenant_id, created_at DESC);

CREATE INDEX ix_room_tenant_status          ON room (tenant_id, status);

CREATE INDEX ix_bill_tenant_status          ON hospital_bill (tenant_id, status);
CREATE INDEX ix_bill_tenant_created         ON hospital_bill (tenant_id, created_at DESC);

CREATE INDEX ix_claim_tenant_status         ON insurance_claim (tenant_id, status);
CREATE INDEX ix_claim_tenant_created        ON insurance_claim (tenant_id, created_at DESC);

CREATE INDEX ix_policy_tenant_status        ON insurance_policy (tenant_id, status);
CREATE INDEX ix_lab_order_tenant_status     ON lab_order (tenant_id, status);
CREATE INDEX ix_prescription_tenant_status  ON pharmacy_prescription (tenant_id, status);
CREATE INDEX ix_user_tenant                 ON app_user (tenant_id);

-- ---- 2. Foreign-key covering indexes --------------------------------
CREATE INDEX ix_doctor_department           ON doctor (department_id);
CREATE INDEX ix_patient_contact_patient     ON patient_contact (patient_id);
CREATE INDEX ix_admission_patient           ON admission (patient_id);
CREATE INDEX ix_admission_doctor            ON admission (doctor_id);
CREATE INDEX ix_admission_type              ON admission (admission_type_id);
CREATE INDEX ix_allocation_admission        ON room_allocation (admission_id);
CREATE INDEX ix_policy_patient              ON insurance_policy (patient_id);
CREATE INDEX ix_policy_company              ON insurance_policy (company_id);
CREATE INDEX ix_lab_order_admission         ON lab_order (admission_id);
CREATE INDEX ix_lab_order_doctor            ON lab_order (doctor_id);
CREATE INDEX ix_lab_item_order              ON lab_order_item (lab_order_id);
CREATE INDEX ix_lab_item_test               ON lab_order_item (lab_test_id);
CREATE INDEX ix_prescription_admission      ON pharmacy_prescription (admission_id);
CREATE INDEX ix_prescription_doctor         ON pharmacy_prescription (doctor_id);
CREATE INDEX ix_prescription_item_rx        ON prescription_item (prescription_id);
CREATE INDEX ix_prescription_item_medicine  ON prescription_item (medicine_id);
CREATE INDEX ix_bill_admission              ON hospital_bill (admission_id);
CREATE INDEX ix_bill_item_bill              ON bill_item (hospital_bill_id);
CREATE INDEX ix_payment_bill                ON payment (hospital_bill_id);
CREATE INDEX ix_refund_payment              ON refund (payment_id);
CREATE INDEX ix_claim_bill                  ON insurance_claim (hospital_bill_id);
CREATE INDEX ix_claim_admission             ON insurance_claim (admission_id);
CREATE INDEX ix_claim_company               ON insurance_claim (company_id);
CREATE INDEX ix_claim_policy                ON insurance_claim (policy_id);
CREATE INDEX ix_claim_item_claim            ON claim_item (claim_id);
CREATE INDEX ix_claim_item_bill_item        ON claim_item (bill_item_id);
CREATE INDEX ix_claim_history_claim         ON claim_status_history (claim_id);

-- ---- 3. Bed allocation: temporal overlap ----------------------------
-- B-tree index used by the availability query:
--   WHERE room_id = ? AND from_date < :to AND (to_date IS NULL OR to_date > :from)
CREATE INDEX ix_allocation_room_period      ON room_allocation (room_id, from_date, to_date);

-- Only one open allocation (patient currently in the bed) per bed.
CREATE UNIQUE INDEX ux_allocation_room_open ON room_allocation (room_id) WHERE to_date IS NULL;

-- Belt-and-braces: the database itself rejects overlapping stays for the same
-- bed, even if two requests race past the application-level check.
CREATE EXTENSION IF NOT EXISTS btree_gist;
ALTER TABLE room_allocation
    ADD CONSTRAINT ex_allocation_no_overlap
    EXCLUDE USING gist (
        room_id WITH =,
        tsrange(from_date, COALESCE(to_date, 'infinity'::timestamp), '[)') WITH &&
    );
