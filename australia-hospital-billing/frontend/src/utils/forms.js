/** Converts '' to null so optional fields don't trip backend @Pattern validation. */
export const clean = (obj) =>
  Object.fromEntries(Object.entries(obj).map(([k, v]) => [k, typeof v === 'string' && v.trim() === '' ? null : v]));

export const AU_STATES = ['NSW', 'VIC', 'QLD', 'WA', 'SA', 'TAS', 'ACT', 'NT'];

/** Australian phone: 04XXXXXXXX mobile or 0[2378]XXXXXXXX landline, optionally +61 instead of 0. */
export const AU_PHONE_RE = /^(\+61|0)[2-478]\d{8}$/;
export const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

/**
 * Medicare card check digit (same algorithm as the backend MedicareNumberValidator):
 * first digit 2-6, weights 1,3,7,9,1,3,7,9 on digits 1-8, sum mod 10 = digit 9, digit 10 = issue no.
 */
export function isValidMedicare(value) {
  const digits = (value || '').replace(/\s/g, '');
  if (!/^[2-6]\d{9}$/.test(digits)) return false;
  const weights = [1, 3, 7, 9, 1, 3, 7, 9];
  const sum = weights.reduce((acc, w, i) => acc + w * Number(digits[i]), 0);
  return sum % 10 === Number(digits[8]);
}

/** Client-side mirror of PatientRequest validation; returns { field: message }. */
export function validatePatient(form) {
  const e = {};
  const name = /^[\p{L}][\p{L} .'-]*$/u;
  if (!form.firstName.trim()) e.firstName = 'First name is required';
  else if (!name.test(form.firstName.trim())) e.firstName = 'Letters, spaces, apostrophes and hyphens only';
  if (!form.lastName.trim()) e.lastName = 'Last name is required';
  else if (!name.test(form.lastName.trim())) e.lastName = 'Letters, spaces, apostrophes and hyphens only';
  if (!form.dob) e.dob = 'Date of birth is required';
  else {
    const dob = new Date(form.dob);
    const oldest = new Date();
    oldest.setFullYear(oldest.getFullYear() - 130);
    if (dob >= new Date()) e.dob = 'Date of birth must be in the past';
    else if (dob < oldest) e.dob = 'Date of birth is more than 130 years ago';
  }
  const phone = form.phone.replace(/\s/g, '');
  if (phone && !AU_PHONE_RE.test(phone)) e.phone = 'Australian number, e.g. 0412345678 or 0291234567';
  if (form.email && !EMAIL_RE.test(form.email.trim())) e.email = 'Enter a valid email';
  if (form.postcode && !/^\d{4}$/.test(form.postcode)) e.postcode = '4 digits';
  const medicare = form.medicareNo.replace(/\s/g, '');
  if (medicare && !isValidMedicare(medicare)) e.medicareNo = 'Invalid Medicare number (10 digits, check digit fails)';
  const irn = form.medicareIrn === '' || form.medicareIrn === null ? null : Number(form.medicareIrn);
  if (medicare && irn === null) e.medicareIrn = 'IRN is required with a Medicare number';
  if (irn !== null && (!Number.isInteger(irn) || irn < 1 || irn > 9)) e.medicareIrn = 'IRN is 1-9';
  if (!medicare && irn !== null) e.medicareIrn = 'Enter the Medicare number too';
  return e;
}
