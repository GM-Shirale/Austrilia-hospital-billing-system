/**
 * Mirrors the backend @PreAuthorize rules so the UI only shows what the user may do.
 * The backend remains the source of truth: hiding a button is UX, not security.
 */
export const ROLES = ['ADMIN', 'BILLING_OFFICER', 'DOCTOR', 'RECEPTIONIST', 'LAB', 'PHARMACY'];

/** Roles offered on the registration screen (the backend enforces the same list). */
export const REGISTRATION_ROLES = [
  { value: 'RECEPTIONIST', label: 'Receptionist', hint: 'Registers patients, admits and discharges' },
  { value: 'DOCTOR', label: 'Doctor', hint: 'Consultations, lab orders and prescriptions' },
  { value: 'BILLING_OFFICER', label: 'Billing Officer', hint: 'Bills, payments, claims and tax invoices' },
  { value: 'LAB', label: 'Lab Technician', hint: 'Lab queue: collect samples, record results' },
  { value: 'PHARMACY', label: 'Pharmacist', hint: 'Pharmacy queue: dispense, stock and formulary' },
  { value: 'ADMIN', label: 'Administrator', hint: 'Full access incl. rooms, doctors and payers' },
];

export const roleLabel = (role) =>
  ({ ADMIN: 'Administrator', BILLING_OFFICER: 'Billing Officer', DOCTOR: 'Doctor', RECEPTIONIST: 'Receptionist',
    LAB: 'Lab Scientist', PHARMACY: 'Pharmacist' })[role] ?? role;

export const can = {
  editPatients: ['ADMIN', 'RECEPTIONIST'],
  editPolicies: ['ADMIN', 'BILLING_OFFICER', 'RECEPTIONIST'],
  admit: ['ADMIN', 'RECEPTIONIST', 'DOCTOR'],
  consult: ['ADMIN', 'DOCTOR', 'RECEPTIONIST'],
  cancelConsult: ['ADMIN', 'DOCTOR'],
  changeDoctor: ['ADMIN', 'RECEPTIONIST', 'DOCTOR'],
  manageSchedules: ['ADMIN', 'RECEPTIONIST', 'DOCTOR'],
  viewInvoice: ['ADMIN', 'BILLING_OFFICER', 'RECEPTIONIST', 'DOCTOR'],
  allocateBed: ['ADMIN', 'RECEPTIONIST'],
  cancelAdmission: ['ADMIN', 'RECEPTIONIST'],
  manageRooms: ['ADMIN'],
  manageProviders: ['ADMIN'],
  orderLab: ['ADMIN', 'DOCTOR', 'LAB'],
  processLab: ['ADMIN', 'LAB', 'DOCTOR'],
  manageLabTests: ['ADMIN', 'LAB'],
  prescribe: ['ADMIN', 'DOCTOR'],
  dispense: ['ADMIN', 'PHARMACY', 'DOCTOR'],
  manageMedicines: ['ADMIN', 'PHARMACY'],
  billing: ['ADMIN', 'BILLING_OFFICER'],
  managePayers: ['ADMIN', 'BILLING_OFFICER'],
};

export const hasRole = (user, allowed) => Boolean(user && (!allowed || allowed.includes(user.role)));
