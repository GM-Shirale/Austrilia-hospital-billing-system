import { useState } from 'react';
import Modal from '../../components/ui/Modal';
import Button from '../../components/ui/Button';
import Field from '../../components/ui/Field';
import { patientApi } from '../../api';
import useAction from '../../hooks/useAction';
import { AU_STATES, clean, validatePatient } from '../../utils/forms';

const EMPTY = {
  firstName: '', lastName: '', dob: '', gender: 'FEMALE', phone: '', email: '', address: '', suburb: '',
  state: 'NSW', postcode: '', medicareNo: '', medicareIrn: '', status: 'ACTIVE',
};

/** Create (patient = undefined) or edit a patient. */
export default function PatientForm({ open, patient, onClose, onSaved }) {
  const [form, setForm] = useState(() =>
    patient
      ? { ...EMPTY, ...Object.fromEntries(Object.entries(patient).map(([k, v]) => [k, v ?? ''])) }
      : EMPTY,
  );
  const [showErrors, setShowErrors] = useState(false);
  const { busy, run } = useAction();
  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));
  const errors = validatePatient(form);
  const err = (key) => showErrors && errors[key] && <span className="mt-1 block text-xs text-rose-600">{errors[key]}</span>;
  const today = new Date().toISOString().slice(0, 10);

  const save = async () => {
    setShowErrors(true);
    if (Object.keys(errors).length) return;
    const body = clean({
      firstName: form.firstName.trim(), lastName: form.lastName.trim(), dob: form.dob, gender: form.gender,
      phone: form.phone.replace(/\s/g, ''), email: form.email.trim(), address: form.address, suburb: form.suburb,
      state: form.state, postcode: form.postcode,
      medicareNo: form.medicareNo.replace(/\s/g, ''), medicareIrn: form.medicareIrn === '' ? null : Number(form.medicareIrn),
      status: form.status,
    });
    const saved = await run(
      () => (patient ? patientApi.update(patient.id, body) : patientApi.create(body)),
      patient ? 'Patient updated' : 'Patient registered',
    );
    if (saved) onSaved(saved);
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      size="max-w-2xl"
      title={patient ? `Edit ${patient.fullName}` : 'Register patient'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button loading={busy} onClick={save}>Save</Button>
        </>
      }
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="First name" required><input className="input" maxLength={60} value={form.firstName} onChange={set('firstName')} />{err('firstName')}</Field>
        <Field label="Last name" required><input className="input" maxLength={60} value={form.lastName} onChange={set('lastName')} />{err('lastName')}</Field>
        <Field label="Date of birth" required><input type="date" className="input" max={today} value={form.dob} onChange={set('dob')} />{err('dob')}</Field>
        <Field label="Gender" required>
          <select className="input" value={form.gender} onChange={set('gender')}>
            {['FEMALE', 'MALE', 'OTHER', 'UNKNOWN'].map((g) => <option key={g}>{g}</option>)}
          </select>
        </Field>
        <Field label="Phone" hint="e.g. 0412345678"><input className="input" inputMode="tel" value={form.phone} onChange={set('phone')} />{err('phone')}</Field>
        <Field label="Email"><input type="email" className="input" value={form.email} onChange={set('email')} />{err('email')}</Field>
        <Field label="Address" className="sm:col-span-2"><input className="input" value={form.address} onChange={set('address')} /></Field>
        <Field label="Suburb"><input className="input" value={form.suburb} onChange={set('suburb')} /></Field>
        <div className="grid grid-cols-2 gap-4">
          <Field label="State">
            <select className="input" value={form.state} onChange={set('state')}>
              {AU_STATES.map((s) => <option key={s}>{s}</option>)}
            </select>
          </Field>
          <Field label="Postcode"><input className="input" inputMode="numeric" value={form.postcode} onChange={set('postcode')} maxLength={4} />{err('postcode')}</Field>
        </div>
        <Field label="Medicare card number" hint="10 digits, validated with the check digit (e.g. 2987001051)">
          <input className="input" inputMode="numeric" value={form.medicareNo} onChange={set('medicareNo')} maxLength={12} />
          {err('medicareNo')}
        </Field>
        <Field label="Medicare IRN" hint="Position on the card (1-9)">
          <input type="number" min={1} max={9} className="input" value={form.medicareIrn} onChange={set('medicareIrn')} />
          {err('medicareIrn')}
        </Field>
        {patient && (
          <Field label="Status">
            <select className="input" value={form.status} onChange={set('status')}>
              {['ACTIVE', 'INACTIVE', 'DECEASED'].map((s) => <option key={s}>{s}</option>)}
            </select>
          </Field>
        )}
      </div>
    </Modal>
  );
}
