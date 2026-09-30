import { useCallback, useEffect, useState } from 'react';
import { BedDouble, RefreshCw } from 'lucide-react';
import Modal from '../../components/ui/Modal';
import Button from '../../components/ui/Button';
import Field from '../../components/ui/Field';
import PatientPicker from '../../components/PatientPicker';
import { admissionApi, providerApi, roomApi } from '../../api';
import useAction from '../../hooks/useAction';
import { label, money } from '../../utils/format';

const CARE_SETTINGS = [
  { value: 'IPD', title: 'In-patient (IPD)', text: 'Admitted, occupies a bed. Medicare 75% of MBS fee.' },
  { value: 'OPD', title: 'Out-patient (OPD)', text: 'Clinic visit, no bed. Medicare 85% of MBS fee.' },
];

/**
 * Admit a patient. For in-patients a bed is picked from the LIVE list of AVAILABLE beds
 * (re-polled every 10 s); the backend locks the bed row and flips it to OCCUPIED.
 */
export default function AdmitForm({ open, patient: preset, onClose, onSaved }) {
  const [patient, setPatient] = useState(preset || null);
  const [doctors, setDoctors] = useState([]);
  const [types, setTypes] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [roomsUpdated, setRoomsUpdated] = useState(null);
  const [form, setForm] = useState({
    doctorId: '', admissionTypeId: '', financialClass: 'PRIVATE', careSetting: 'IPD', diagnosis: '', roomId: '',
  });
  const [submitted, setSubmitted] = useState(false);
  const { busy, run } = useAction();
  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  const loadRooms = useCallback(() => {
    roomApi.list('AVAILABLE')
      .then((list) => {
        setRooms(list);
        setRoomsUpdated(new Date());
        // A bed taken at another desk disappears from the list: drop the stale selection.
        setForm((f) => (f.roomId && !list.some((r) => String(r.id) === String(f.roomId)) ? { ...f, roomId: '' } : f));
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    Promise.all([providerApi.doctors(undefined, 0, 100), admissionApi.types()])
      .then(([doctorPage, typeList]) => {
        const active = doctorPage.content.filter((d) => d.active);
        setDoctors(active);
        setTypes(typeList);
        setForm((f) => ({
          ...f,
          doctorId: active[0]?.id ?? '',
          admissionTypeId: typeList.find((t) => t.code === 'ELECTIVE')?.id ?? typeList[0]?.id ?? '',
        }));
      })
      .catch(() => {});
    loadRooms();
    const timer = setInterval(loadRooms, 10000);
    return () => clearInterval(timer);
  }, [loadRooms]);

  const inpatient = form.careSetting === 'IPD';
  const bedMissing = inpatient && !form.roomId;

  const save = async () => {
    setSubmitted(true);
    if (!patient || !form.doctorId || bedMissing) return;
    const saved = await run(
      () =>
        admissionApi.admit({
          patientId: patient.id,
          doctorId: Number(form.doctorId),
          admissionTypeId: Number(form.admissionTypeId),
          financialClass: form.financialClass,
          careSetting: form.careSetting,
          diagnosis: form.diagnosis.trim() || null,
          roomId: inpatient && form.roomId ? Number(form.roomId) : null,
        }),
      inpatient ? 'Patient admitted - bed now OCCUPIED' : 'Out-patient visit opened',
    );
    if (saved) onSaved(saved);
    else loadRooms();          // e.g. the bed was just taken: refresh the choices
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Admit patient"
      size="max-w-2xl"
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button loading={busy} disabled={!form.doctorId} onClick={save}>
            {inpatient ? 'Admit & occupy bed' : 'Open OPD visit'}
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        {/* A <div>, not a <label>: a label would forward the result click to the "change" button. */}
        <div>
          <span className="label">Patient <span className="text-rose-500">*</span></span>
          {patient ? (
            <div className="flex items-center justify-between rounded-lg border border-slate-200 px-3 py-2 text-sm">
              <span>
                <b>{patient.fullName}</b> <span className="text-slate-500">{patient.mrn}</span>
              </span>
              {!preset && (
                <button type="button" className="text-xs text-blue-600" onClick={() => setPatient(null)}>change</button>
              )}
            </div>
          ) : (
            <PatientPicker onSelect={setPatient} />
          )}
          {submitted && !patient && <p className="mt-1 text-xs text-rose-600">Search and select a patient</p>}
        </div>

        <div className="grid gap-2 sm:grid-cols-2">
          {CARE_SETTINGS.map((c) => (
            <label
              key={c.value}
              className={`flex cursor-pointer gap-3 rounded-lg border p-3 text-sm ${form.careSetting === c.value ? 'border-blue-500 bg-blue-50 ring-1 ring-blue-500' : 'border-slate-200 hover:border-blue-300'}`}
            >
              <input type="radio" name="careSetting" value={c.value} checked={form.careSetting === c.value} onChange={set('careSetting')} className="mt-0.5" />
              <span>
                <span className="block font-semibold text-slate-800">{c.title}</span>
                <span className="block text-xs text-slate-500">{c.text}</span>
              </span>
            </label>
          ))}
        </div>

        <div className="grid gap-4 sm:grid-cols-3">
          <Field label="Attending doctor" required>
            <select className="input" value={form.doctorId} onChange={set('doctorId')}>
              {doctors.map((d) => <option key={d.id} value={d.id}>{d.displayName} - {d.specialization}</option>)}
            </select>
          </Field>
          <Field label="Admission type" required>
            <select className="input" value={form.admissionTypeId} onChange={set('admissionTypeId')}>
              {types.map((t) => <option key={t.id} value={t.id}>{t.typeName}</option>)}
            </select>
          </Field>
          <Field label="Financial class" hint="Public = 100% funded">
            <select className="input" value={form.financialClass} onChange={set('financialClass')}>
              <option value="PRIVATE">Private patient</option>
              <option value="PUBLIC">Public patient</option>
            </select>
          </Field>
        </div>

        {inpatient && (
          <div>
            <div className="mb-1 flex items-center justify-between">
              <span className="label">Available beds <span className="text-rose-500">*</span></span>
              <button type="button" onClick={loadRooms} className="flex items-center gap-1 text-xs text-blue-600 hover:underline">
                <RefreshCw className="h-3 w-3" /> {roomsUpdated ? `live - updated ${roomsUpdated.toLocaleTimeString('en-AU')}` : 'refresh'}
              </button>
            </div>
            {rooms.length === 0 ? (
              <p className="rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-700">No beds are AVAILABLE right now. Discharge a patient or bring a bed back from maintenance.</p>
            ) : (
              <div className="grid max-h-56 gap-2 overflow-y-auto sm:grid-cols-3" role="radiogroup" aria-label="Available beds">
                {rooms.map((r) => {
                  const selected = String(form.roomId) === String(r.id);
                  return (
                    <button
                      key={r.id}
                      type="button"
                      role="radio"
                      aria-checked={selected}
                      onClick={() => setForm((f) => ({ ...f, roomId: String(r.id) }))}
                      className={`rounded-lg border p-2 text-left text-sm ${selected ? 'border-blue-500 bg-blue-50 ring-1 ring-blue-500' : 'border-emerald-200 hover:border-blue-300'}`}
                    >
                      <span className="flex items-center gap-1 font-semibold"><BedDouble className="h-4 w-4 text-emerald-600" /> {r.roomNo}/{r.bedNo}</span>
                      <span className="block text-xs text-slate-500">{label(r.roomType)} - {money(r.dailyRate)}/day</span>
                    </button>
                  );
                })}
              </div>
            )}
            {submitted && bedMissing && <p className="mt-1 text-xs text-rose-600">Select a bed for the in-patient admission</p>}
          </div>
        )}

        <Field label="Diagnosis / reason">
          <textarea className="input" rows={2} maxLength={500} value={form.diagnosis} onChange={set('diagnosis')} />
        </Field>
      </div>
    </Modal>
  );
}
