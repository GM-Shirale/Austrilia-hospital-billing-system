import client from './client';

const data = (promise) => promise.then((r) => r.data);

/** Removes empty values so they are not sent as query parameters. */
const params = (obj = {}) =>
  Object.fromEntries(Object.entries(obj).filter(([, v]) => v !== undefined && v !== null && v !== ''));

export const authApi = {
  hospitals: () => data(client.get('/hospitals')),
  login: (tenantId, username, password) =>
    data(client.post('/auth/login', { username, password }, { headers: { 'X-Tenant-ID': tenantId } })),
  me: () => data(client.get('/auth/me')),
  logout: () => client.post('/auth/logout'),
  registrationOptions: () => data(client.get('/auth/register/options')),
  register: (body) =>
    data(client.post('/auth/register', body, { headers: { 'X-Tenant-ID': body.hospitalId } })),
};

export const dashboardApi = {
  summary: () => data(client.get('/dashboard/summary')),
};

export const patientApi = {
  search: (q, page = 0, size = 10) => data(client.get('/patients', { params: params({ q, page, size }) })),
  get: (id) => data(client.get(`/patients/${id}`)),
  create: (body) => data(client.post('/patients', body)),
  update: (id, body) => data(client.put(`/patients/${id}`, body)),
  addContact: (id, body) => data(client.post(`/patients/${id}/contacts`, body)),
  removeContact: (id, contactId) => data(client.delete(`/patients/${id}/contacts/${contactId}`)),
  admissions: (id) => data(client.get(`/patients/${id}/admissions`)),
  policies: (id) => data(client.get(`/patients/${id}/policies`)),
  addPolicy: (id, body) => data(client.post(`/patients/${id}/policies`, body)),
};

export const insuranceApi = {
  companies: () => data(client.get('/insurance/companies')),
  createCompany: (body) => data(client.post('/insurance/companies', body)),
  changePolicyStatus: (policyId, status) => data(client.patch(`/insurance/policies/${policyId}/status`, { status })),
};

export const providerApi = {
  departments: () => data(client.get('/departments')),
  createDepartment: (body) => data(client.post('/departments', body)),
  doctors: (departmentId, page = 0, size = 50) =>
    data(client.get('/doctors', { params: params({ departmentId, page, size }) })),
  createDoctor: (body) => data(client.post('/doctors', body)),
  updateDoctor: (id, body) => data(client.put(`/doctors/${id}`, body)),
  allSchedules: () => data(client.get('/doctor-schedules')),
  // Guard: never call /doctors//schedules when no doctor is selected yet.
  schedules: (doctorId) => (doctorId ? data(client.get(`/doctors/${doctorId}/schedules`)) : Promise.resolve([])),
  addSchedule: (doctorId, body) => data(client.post(`/doctors/${doctorId}/schedules`, body)),
  updateSchedule: (id, body) => data(client.put(`/doctor-schedules/${id}`, body)),
  deleteSchedule: (id) => client.delete(`/doctor-schedules/${id}`),
};

export const admissionApi = {
  search: (status, page = 0, size = 10) => data(client.get('/admissions', { params: params({ status, page, size }) })),
  get: (id) => data(client.get(`/admissions/${id}`)),
  types: () => data(client.get('/admission-types')),
  admit: (body) => data(client.post('/admissions', body)),
  discharge: (id, dischargeDate) => data(client.post(`/admissions/${id}/discharge`, dischargeDate ? { dischargeDate } : {})),
  cancel: (id) => data(client.post(`/admissions/${id}/cancel`)),
  allocateBed: (id, roomId) => data(client.post(`/admissions/${id}/bed`, { roomId })),
  changeDoctor: (id, doctorId) => data(client.put(`/admissions/${id}/doctor`, { doctorId })),
  consultations: (id) => data(client.get(`/admissions/${id}/consultations`)),
  addConsultation: (id, body) => data(client.post(`/admissions/${id}/consultations`, body)),
  cancelConsultation: (consultationId) => data(client.post(`/consultations/${consultationId}/cancel`)),
  bills: (id) => data(client.get(`/admissions/${id}/bills`)),
};

export const roomApi = {
  list: (status) => data(client.get('/rooms', { params: params({ status }) })),
  create: (body) => data(client.post('/rooms', body)),
  update: (id, body) => data(client.put(`/rooms/${id}`, body)),
  remove: (id) => client.delete(`/rooms/${id}`),
};

export const labApi = {
  tests: (activeOnly = false) => data(client.get('/lab/tests', { params: params({ activeOnly }) })),
  createTest: (body) => data(client.post('/lab/tests', body)),
  updateTest: (id, body) => data(client.put(`/lab/tests/${id}`, body)),
  queue: () => data(client.get('/lab/queue')),
  orders: (status, page = 0, size = 10) => data(client.get('/lab/orders', { params: params({ status, page, size }) })),
  order: (id) => data(client.get(`/lab/orders/${id}`)),
  forAdmission: (admissionId) => data(client.get(`/lab/orders/by-admission/${admissionId}`)),
  createOrder: (body) => data(client.post('/lab/orders', body)),
  changeStatus: (id, status, notes) => data(client.patch(`/lab/orders/${id}/status`, { status, notes: notes || null })),
  recordResult: (id, itemId, result) => data(client.put(`/lab/orders/${id}/items/${itemId}/result`, { result })),
};

export const pharmacyApi = {
  medicines: (activeOnly = false) => data(client.get('/pharmacy/medicines', { params: params({ activeOnly }) })),
  createMedicine: (body) => data(client.post('/pharmacy/medicines', body)),
  updateMedicine: (id, body) => data(client.put(`/pharmacy/medicines/${id}`, body)),
  adjustStock: (id, delta, reason) => data(client.patch(`/pharmacy/medicines/${id}/stock`, { delta, reason: reason || null })),
  movements: (id) => data(client.get(`/pharmacy/medicines/${id}/movements`)),
  queue: () => data(client.get('/pharmacy/queue')),
  prescriptions: (status, page = 0, size = 10) =>
    data(client.get('/pharmacy/prescriptions', { params: params({ status, page, size }) })),
  prescription: (id) => data(client.get(`/pharmacy/prescriptions/${id}`)),
  forAdmission: (admissionId) => data(client.get(`/pharmacy/prescriptions/by-admission/${admissionId}`)),
  prescribe: (body) => data(client.post('/pharmacy/prescriptions', body)),
  dispense: (id) => data(client.post(`/pharmacy/prescriptions/${id}/dispense`)),
  cancel: (id) => data(client.post(`/pharmacy/prescriptions/${id}/cancel`)),
};

export const billingApi = {
  search: (status, page = 0, size = 10, admissionId) =>
    data(client.get('/bills', { params: params({ status, page, size, admissionId }) })),
  get: (id) => data(client.get(`/bills/${id}`)),
  invoice: (id) => data(client.get(`/bills/${id}/invoice`)),
  generate: (admissionId) => data(client.post('/bills', { admissionId })),
  recalculate: (id) => data(client.post(`/bills/${id}/recalculate`)),
  addItem: (id, body) => data(client.post(`/bills/${id}/items`, body)),
  removeItem: (id, itemId) => data(client.delete(`/bills/${id}/items/${itemId}`)),
  finalize: (id) => data(client.post(`/bills/${id}/finalize`)),
  cancel: (id) => data(client.post(`/bills/${id}/cancel`)),
  payments: (id) => data(client.get(`/bills/${id}/payments`)),
  pay: (id, body) => data(client.post(`/bills/${id}/payments`, body)),
  refund: (paymentId, body) => data(client.post(`/payments/${paymentId}/refunds`, body)),
};

export const claimApi = {
  search: (status, page = 0, size = 10) => data(client.get('/claims', { params: params({ status, page, size }) })),
  get: (id) => data(client.get(`/claims/${id}`)),
  forBill: (billId) => data(client.get(`/bills/${billId}/claims`)),
  createForBill: (billId) => data(client.post(`/bills/${billId}/claims`)),
  validate: (id) => data(client.post(`/claims/${id}/validate`)),
  submit: (id) => data(client.post(`/claims/${id}/submit`)),
  close: (id, note) => data(client.post(`/claims/${id}/close`, { note })),
};

/** Spring AI features (every endpoint falls back to deterministic rules when AI is disabled). */
export const aiApi = {
  status: () => data(client.get('/ai/status')),
  mbsSuggestions: (clinicalNotes, specialty) =>
    data(client.post('/coding/mbs-suggestions', { clinicalNotes, specialty }, { timeout: 60000 })),
  explainBill: (billId) => data(client.get(`/bills/${billId}/ai-explanation`, { timeout: 60000 })),
  reviewClaim: (claimId) => data(client.get(`/claims/${claimId}/ai-review`, { timeout: 60000 })),
};
