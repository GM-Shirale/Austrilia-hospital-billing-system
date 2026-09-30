-- =====================================================================
-- V3: Demo data (two hospitals to demonstrate tenant isolation)
-- Fees and MBS values are illustrative, not the current official schedule.
--
-- Demo logins (password in brackets):
--   Sydney Harbour Private : admin (Admin@123), billing (Billing@123),
--                            doctor (Doctor@123), reception (Reception@123),
--                            lab (Lab@123), pharmacy (Pharmacy@123)
--   Melbourne General      : melb.admin (Admin@123)
-- =====================================================================

INSERT INTO hospital (id, code, name, hospital_type, abn, state) VALUES
 ('11111111-1111-4111-8111-111111111111', 'SYD-HARBOUR', 'Sydney Harbour Private Hospital', 'PRIVATE', '51824753556', 'NSW'),
 ('22222222-2222-4222-8222-222222222222', 'MEL-GENERAL', 'Melbourne General Hospital',      'PUBLIC',  '33051775556', 'VIC');

INSERT INTO app_user (tenant_id, username, password_hash, full_name, email, role) VALUES
 ('11111111-1111-4111-8111-111111111111', 'admin',      '$2a$10$8/EjU.LvsuzziAhFfzw3/ejR587u4OiDhE2DRP3uV.NRi7JXfaz5y', 'Olivia Carter',    'admin@harbour.example.au',     'ADMIN'),
 ('11111111-1111-4111-8111-111111111111', 'billing',    '$2a$10$jLX4VCp3mvMnqQbwer14E.Byh5LpaU2sD0oQyEKJKxsEJedSS2G3O', 'Liam Nguyen',      'billing@harbour.example.au',   'BILLING'),
 ('11111111-1111-4111-8111-111111111111', 'doctor',     '$2a$10$251wvuqO03EWBaSFg.avb.MQ0zDczzZMB9fiV4MVk7l4o/mIaMfo6', 'Dr Sarah Mitchell','s.mitchell@harbour.example.au','DOCTOR'),
 ('11111111-1111-4111-8111-111111111111', 'reception',  '$2a$10$nZONftfHcafweolbnzPmzOuPMzwXVF7TPhWTcv.WI5GNrB4GBIcAe', 'Chloe Wilson',     'frontdesk@harbour.example.au', 'RECEPTIONIST'),
 ('11111111-1111-4111-8111-111111111111', 'lab',        '$2a$10$e4nsoWUugIdHao7wsh7wL.YBMWEW4CN8OIzZ4owTrMjeuXB7tY3CO', 'Noah Patel',       'lab@harbour.example.au',       'LAB'),
 ('11111111-1111-4111-8111-111111111111', 'pharmacy',   '$2a$10$AxqJLI6GBpq9qv6oH.F6ou47umL4FYedxxjVbRIu86GbrgXNFqfrK', 'Mia Thompson',     'pharmacy@harbour.example.au',  'PHARMACY'),
 ('22222222-2222-4222-8222-222222222222', 'melb.admin', '$2a$10$8/EjU.LvsuzziAhFfzw3/ejR587u4OiDhE2DRP3uV.NRi7JXfaz5y', 'Jack Robinson',    'admin@melbgen.example.au',     'ADMIN');

-- ---------------------------------------------------------------------
-- Sydney Harbour Private (tenant 1111...)
-- ---------------------------------------------------------------------
INSERT INTO department (tenant_id, department_name, location) VALUES
 ('11111111-1111-4111-8111-111111111111', 'Cardiology',       'Level 3, East Wing'),
 ('11111111-1111-4111-8111-111111111111', 'Orthopaedics',     'Level 2, West Wing'),
 ('11111111-1111-4111-8111-111111111111', 'General Medicine', 'Level 1, Main Building'),
 ('11111111-1111-4111-8111-111111111111', 'Emergency',        'Ground Floor');

INSERT INTO doctor (tenant_id, department_id, first_name, last_name, specialization, phone, email,
                    provider_no, consultation_fee, mbs_item_no, mbs_schedule_fee)
SELECT '11111111-1111-4111-8111-111111111111', d.id, v.first_name, v.last_name, v.spec, v.phone, v.email,
       v.provider_no, v.fee, v.item, v.sched
FROM (VALUES
  ('Cardiology',       'Sarah',  'Mitchell', 'Interventional Cardiologist', '0291110001', 's.mitchell@harbour.example.au', '2451731J', 320.00, '110', 170.50),
  ('Orthopaedics',     'James',  'O''Brien', 'Orthopaedic Surgeon',         '0291110002', 'j.obrien@harbour.example.au',   '2987654K', 280.00, '104', 98.95),
  ('General Medicine', 'Priya',  'Sharma',   'General Physician',           '0291110003', 'p.sharma@harbour.example.au',   '3123456T', 180.00, '116', 85.55),
  ('Emergency',        'Daniel', 'Kim',      'Emergency Physician',         '0291110004', 'd.kim@harbour.example.au',      '4567890F', 150.00, '105', 49.80)
) AS v(dept, first_name, last_name, spec, phone, email, provider_no, fee, item, sched)
JOIN department d ON d.department_name = v.dept AND d.tenant_id = '11111111-1111-4111-8111-111111111111';

INSERT INTO admission_type (tenant_id, code, type_name) VALUES
 ('11111111-1111-4111-8111-111111111111', 'EMERGENCY',   'Emergency'),
 ('11111111-1111-4111-8111-111111111111', 'ELECTIVE',    'Elective (planned)'),
 ('11111111-1111-4111-8111-111111111111', 'DAY_SURGERY', 'Day Surgery'),
 ('11111111-1111-4111-8111-111111111111', 'MATERNITY',   'Maternity');

INSERT INTO room (tenant_id, room_no, bed_no, room_type, daily_rate, status) VALUES
 ('11111111-1111-4111-8111-111111111111', '101', 'A', 'GENERAL_WARD', 450.00,  'AVAILABLE'),
 ('11111111-1111-4111-8111-111111111111', '101', 'B', 'GENERAL_WARD', 450.00,  'AVAILABLE'),
 ('11111111-1111-4111-8111-111111111111', '102', 'A', 'GENERAL_WARD', 450.00,  'AVAILABLE'),
 ('11111111-1111-4111-8111-111111111111', '201', 'A', 'SEMI_PRIVATE', 750.00,  'AVAILABLE'),
 ('11111111-1111-4111-8111-111111111111', '201', 'B', 'SEMI_PRIVATE', 750.00,  'AVAILABLE'),
 ('11111111-1111-4111-8111-111111111111', '301', 'A', 'PRIVATE',      1200.00, 'AVAILABLE'),
 ('11111111-1111-4111-8111-111111111111', '302', 'A', 'PRIVATE',      1200.00, 'MAINTENANCE'),
 ('11111111-1111-4111-8111-111111111111', 'ICU1', 'A', 'ICU',         3500.00, 'AVAILABLE');

INSERT INTO insurance_company (tenant_id, company_name, payer_type, network_status, known_gap_cap, abn, contact_person, phone, email) VALUES
 ('11111111-1111-4111-8111-111111111111', 'Medicare Australia', 'MEDICARE',     'NOT_APPLICABLE',    NULL,   NULL,          'Services Australia', '132011',     'medicare@example.gov.au'),
 ('11111111-1111-4111-8111-111111111111', 'Bupa',               'PRIVATE_FUND', 'NO_GAP',            NULL,   '81000057590', 'Provider Relations', '134135',     'providers@bupa.example.au'),
 ('11111111-1111-4111-8111-111111111111', 'Medibank',           'PRIVATE_FUND', 'KNOWN_GAP',         500.00, '47080890259', 'Hospital Contracts', '132331',     'contracts@medibank.example.au'),
 ('11111111-1111-4111-8111-111111111111', 'HCF',                'PRIVATE_FUND', 'NON_PARTICIPATING', NULL,   '68000026746', 'Claims Team',        '131334',     'claims@hcf.example.au');

INSERT INTO patient (tenant_id, mrn, first_name, last_name, dob, gender, phone, email, address, suburb, state, postcode, medicare_no, medicare_irn) VALUES
 ('11111111-1111-4111-8111-111111111111', 'MRN-100001', 'Emily',   'Johnson', '1985-04-12', 'FEMALE', '0412345678', 'emily.j@example.com',   '12 Harbour St',     'Sydney',      'NSW', '2000', '2123456071', 1),
 ('11111111-1111-4111-8111-111111111111', 'MRN-100002', 'William', 'Brown',   '1958-11-03', 'MALE',   '0423456789', 'w.brown@example.com',   '45 George St',      'Parramatta',  'NSW', '2150', '3987654081', 2),
 ('11111111-1111-4111-8111-111111111111', 'MRN-100003', 'Aisha',   'Khan',    '1992-07-25', 'FEMALE', '0434567890', 'aisha.khan@example.com','8 Beach Rd',        'Bondi',       'NSW', '2026', '4555123071', 1),
 ('11111111-1111-4111-8111-111111111111', 'MRN-100004', 'Lucas',   'Martin',  '2001-02-14', 'MALE',   '0445678901', 'lucas.m@example.com',   '101 Pacific Hwy',   'Chatswood',   'NSW', '2067', '5123987011', 1);

INSERT INTO patient_contact (tenant_id, patient_id, name, relation, phone, email, is_primary)
SELECT p.tenant_id, p.id, v.name, v.relation, v.phone, v.email, TRUE
FROM (VALUES
  ('MRN-100001', 'Michael Johnson', 'Spouse',   '0411111111', 'michael.j@example.com'),
  ('MRN-100002', 'Grace Brown',     'Daughter', '0422222222', 'grace.b@example.com')
) AS v(mrn, name, relation, phone, email)
JOIN patient p ON p.mrn = v.mrn AND p.tenant_id = '11111111-1111-4111-8111-111111111111';

INSERT INTO insurance_policy (tenant_id, patient_id, company_id, policy_no, cover_tier, excess_amount, annual_limit, start_date, end_date, status)
SELECT p.tenant_id, p.id, c.id, v.policy_no, v.tier, v.excess, v.annual_limit, v.start_date::date, v.end_date::date, v.status
FROM (VALUES
  ('MRN-100001', 'Bupa',     'BUPA-7788123',  'GOLD',        250.00, 100000.00, '2020-01-01', NULL,         'ACTIVE'),
  ('MRN-100002', 'Medibank', 'MED-5521009',   'SILVER_PLUS', 500.00, 60000.00,  '2018-07-01', NULL,         'ACTIVE'),
  ('MRN-100003', 'HCF',      'HCF-3344556',   'BRONZE',      750.00, 30000.00,  '2022-03-15', NULL,         'ACTIVE'),
  ('MRN-100004', 'Bupa',     'BUPA-1100220',  'BASIC',       750.00, 10000.00,  '2021-01-01', '2024-12-31', 'LAPSED')
) AS v(mrn, company, policy_no, tier, excess, annual_limit, start_date, end_date, status)
JOIN patient p ON p.mrn = v.mrn AND p.tenant_id = '11111111-1111-4111-8111-111111111111'
JOIN insurance_company c ON c.company_name = v.company AND c.tenant_id = p.tenant_id;

INSERT INTO lab_test (tenant_id, test_code, test_name, category, unit_price, mbs_item_no, mbs_schedule_fee) VALUES
 ('11111111-1111-4111-8111-111111111111', 'FBC',   'Full Blood Count',             'Haematology', 45.00,  '65070', 16.95),
 ('11111111-1111-4111-8111-111111111111', 'UEC',   'Urea, Electrolytes, Creatinine','Biochemistry', 55.00, '66512', 17.70),
 ('11111111-1111-4111-8111-111111111111', 'LIPID', 'Lipid Profile',                'Biochemistry', 60.00,  '66536', 19.90),
 ('11111111-1111-4111-8111-111111111111', 'TROP',  'High-sensitivity Troponin',    'Cardiac',      95.00,  '66518', 29.05),
 ('11111111-1111-4111-8111-111111111111', 'XRCH',  'Chest X-Ray',                  'Imaging',      180.00, '58500', 47.15),
 ('11111111-1111-4111-8111-111111111111', 'ECG',   '12-lead ECG',                  'Cardiac',      85.00,  '11714', 32.50);

INSERT INTO medicine (tenant_id, medicine_name, category, unit_price, gst_rate, stock_quantity) VALUES
 ('11111111-1111-4111-8111-111111111111', 'Paracetamol 500mg tablet',      'Analgesic',       0.60,  0.0000, 5000),
 ('11111111-1111-4111-8111-111111111111', 'Amoxicillin 500mg capsule',     'Antibiotic',      1.20,  0.0000, 2000),
 ('11111111-1111-4111-8111-111111111111', 'Atorvastatin 40mg tablet',      'Cardiovascular',  0.95,  0.0000, 3000),
 ('11111111-1111-4111-8111-111111111111', 'Enoxaparin 40mg injection',     'Anticoagulant',   14.50, 0.0000, 400),
 ('11111111-1111-4111-8111-111111111111', 'Ondansetron 4mg tablet',        'Antiemetic',      2.10,  0.0000, 800),
 ('11111111-1111-4111-8111-111111111111', 'Compression stockings (pair)',  'Medical supplies',38.00, 0.1000, 150),
 ('11111111-1111-4111-8111-111111111111', 'Antiseptic wound dressing kit', 'Medical supplies',22.00, 0.1000, 300);

-- ---------------------------------------------------------------------
-- Melbourne General (tenant 2222...) - small dataset, proves isolation
-- ---------------------------------------------------------------------
INSERT INTO department (tenant_id, department_name, location) VALUES
 ('22222222-2222-4222-8222-222222222222', 'General Medicine', 'Block A');

INSERT INTO doctor (tenant_id, department_id, first_name, last_name, specialization, phone, email,
                    provider_no, consultation_fee, mbs_item_no, mbs_schedule_fee)
SELECT '22222222-2222-4222-8222-222222222222', d.id, 'Hannah', 'Lee', 'General Physician', '0391110001',
       'h.lee@melbgen.example.au', '5234567B', 160.00, '116', 85.55
FROM department d WHERE d.tenant_id = '22222222-2222-4222-8222-222222222222';

INSERT INTO admission_type (tenant_id, code, type_name) VALUES
 ('22222222-2222-4222-8222-222222222222', 'EMERGENCY', 'Emergency'),
 ('22222222-2222-4222-8222-222222222222', 'ELECTIVE',  'Elective (planned)');

INSERT INTO room (tenant_id, room_no, bed_no, room_type, daily_rate, status) VALUES
 ('22222222-2222-4222-8222-222222222222', 'W1', 'A', 'GENERAL_WARD', 400.00, 'AVAILABLE');

INSERT INTO insurance_company (tenant_id, company_name, payer_type, network_status) VALUES
 ('22222222-2222-4222-8222-222222222222', 'Medicare Australia', 'MEDICARE', 'NOT_APPLICABLE');

INSERT INTO patient (tenant_id, mrn, first_name, last_name, dob, gender, phone, suburb, state, postcode, medicare_no, medicare_irn) VALUES
 ('22222222-2222-4222-8222-222222222222', 'MRN-500001', 'Oliver', 'Taylor', '1979-09-09', 'MALE', '0456789012', 'Carlton', 'VIC', '3053', '2987001051', 1);
