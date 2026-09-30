import { useEffect, useState } from 'react';
import Modal from '../../components/ui/Modal';
import Button from '../../components/ui/Button';
import Field from '../../components/ui/Field';
import { admissionApi, providerApi } from '../../api';
import useAction from '../../hooks/useAction';
import { label, money, toLocalInput } from '../../utils/format';

const TYPES = ['INITIAL', 'FOLLOW_UP', 'SPECIALIST_REVIEW', 'TELEHEALTH'];

/** Records a doctor consultation on an active admission; it is billed with the doctor's MBS item. */
export default function ConsultationForm({ admission, defaultDoctorId, onClose, onSaved }) {
  const [doctors, setDoctors] = useState([]);
  const [form, setForm] = useState({
    doctorId: defaultDoctorId ?? '', consultationType: 'FOLLOW_UP', consultationAt: toLocalInput(), mbsItemNo: '', fee: '', notes: '',
  });
  const [roster, setRoster] = useState([]);
  const { busy, run } = useAction();
  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  useEffect(() => {
    providerApi.doctors(undefined, 0, 100).then((page) => setDoctors(page.content.filter((d) => d.active))).catch(() => {});
  }, []);

  useEffect(() => {
    if (form.doctorId) providerApi.schedules(form.doctorId).then(setRoster).catch(() => setRoster([]));
  }, [form.doctorId]);

  const doctor = doctors.find((d) => String(d.id) === String(form.doctorId));

  const save = async () => {
    const saved = await run(
      () => admissionApi.addConsultation(admission.id, {
        doctorId: Number(form.doctorId),
        consultationType: form.consultationType,
        consultationAt: form.consultationAt || null,
        mbsItemNo: form.mbsItemNo.trim() || null,
        fee: form.fee === '' ? null : Number(form.fee),
        notes: form.notes.trim() || null,
      }),
      'Consultation recorded',
    );
    if (saved) onSaved(saved);
  };

  return (
    <Modal
      open
      onClose={onClose}
      title={`Record consultation - ${admission.admissionNo}`}
      size="max-w-xl"
      footer={<><Button variant="secondary" onClick={onClose}>Cancel</Button><Button loading={busy} disabled={!form.doctorId} onClick={save}>Save consultation</Button></>}
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Doctor" required className="sm:col-span-2">
          <select className="input" value={form.doctorId} onChange={set('doctorId')}>
            <option value="">Select a doctor</option>
            {doctors.map((d) => <option key={d.id} value={d.id}>{d.displayName} - {d.specialization} ({d.providerNo})</option>)}
          </select>
        </Field>
        {doctor && (
          <p className="text-xs text-slate-500 sm:col-span-2">
            Default: MBS item {doctor.mbsItemNo} (schedule fee {money(doctor.mbsScheduleFee)}), consultation fee {money(doctor.consultationFee)}.
            {' '}Roster: {roster.length ? roster.map((r) => `${label(r.dayOfWeek).slice(0, 3)} ${r.startTime.slice(0, 5)}-${r.endTime.slice(0, 5)}`).join(', ') : 'on call (no sessions)'}
          </p>
        )}
        <Field label="Type" required>
          <select className="input" value={form.consultationType} onChange={set('consultationType')}>
            {TYPES.map((t) => <option key={t} value={t}>{label(t)}</option>)}
          </select>
        </Field>
        <Field label="Date & time" required>
          <input type="datetime-local" className="input" value={form.consultationAt} max={toLocalInput(new Date(Date.now() + 3600000))} onChange={set('consultationAt')} />
        </Field>
        <Field label="MBS item (optional)" hint="Leave empty to use the doctor's default">
          <input className="input" inputMode="numeric" maxLength={5} value={form.mbsItemNo} onChange={set('mbsItemNo')} placeholder={doctor?.mbsItemNo} />
        </Field>
        <Field label="Fee (optional, AUD)">
          <input type="number" min="0" step="0.01" className="input" value={form.fee} onChange={set('fee')} placeholder={doctor?.consultationFee} />
        </Field>
        <Field label="Clinical notes" className="sm:col-span-2">
          <textarea className="input" rows={2} maxLength={500} value={form.notes} onChange={set('notes')} />
        </Field>
      </div>
    </Modal>
  );
}
