import { useEffect, useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import Modal from '../../components/ui/Modal';
import Button from '../../components/ui/Button';
import Field from '../../components/ui/Field';
import AdmissionSelect from '../../components/AdmissionSelect';
import { pharmacyApi } from '../../api';
import useAction from '../../hooks/useAction';
import { money } from '../../utils/format';

const blankLine = (medicineId) => ({ medicineId, quantity: 10, dose: '1 tablet', frequency: 'Twice daily' });

export default function PrescriptionForm({ open, admission: preset, onClose, onSaved }) {
  const [admission, setAdmission] = useState(preset || null);
  const [medicines, setMedicines] = useState([]);
  const [lines, setLines] = useState([]);
  const { busy, run } = useAction();

  useEffect(() => {
    pharmacyApi.medicines(true).then((list) => {
      setMedicines(list);
      setLines(list[0] ? [blankLine(list[0].id)] : []);
    });
  }, []);

  const update = (i, patch) => setLines((ls) => ls.map((l, idx) => (idx === i ? { ...l, ...patch } : l)));

  const save = async () => {
    const saved = await run(
      () =>
        pharmacyApi.prescribe({
          admissionId: admission.id,
          doctorId: admission.doctorId,
          items: lines.map((l) => ({ ...l, medicineId: Number(l.medicineId), quantity: Number(l.quantity) })),
        }),
      'Prescription created',
    );
    if (saved) onSaved(saved);
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="New prescription"
      size="max-w-2xl"
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button loading={busy} disabled={!admission || lines.length === 0} onClick={save}>Prescribe</Button>
        </>
      }
    >
      <div className="space-y-4">
        {!preset && (
          <Field label="Admission" required>
            <AdmissionSelect value={admission} onChange={setAdmission} />
          </Field>
        )}
        {lines.map((line, i) => (
          <div key={i} className="grid grid-cols-12 items-end gap-2">
            <Field label={i === 0 ? 'Medicine' : ''} className="col-span-5">
              <select className="input" value={line.medicineId} onChange={(e) => update(i, { medicineId: e.target.value })}>
                {medicines.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.medicineName} - {money(m.unitPrice)} {m.gstFree ? '(GST-free)' : '(+10% GST)'} - stock {m.stockQuantity}
                  </option>
                ))}
              </select>
            </Field>
            <Field label={i === 0 ? 'Qty' : ''} className="col-span-2">
              <input type="number" min={1} className="input" value={line.quantity} onChange={(e) => update(i, { quantity: e.target.value })} />
            </Field>
            <Field label={i === 0 ? 'Dose' : ''} className="col-span-2">
              <input className="input" value={line.dose} onChange={(e) => update(i, { dose: e.target.value })} />
            </Field>
            <Field label={i === 0 ? 'Frequency' : ''} className="col-span-2">
              <input className="input" value={line.frequency} onChange={(e) => update(i, { frequency: e.target.value })} />
            </Field>
            <button type="button" className="col-span-1 mb-2 text-slate-400 hover:text-rose-600" onClick={() => setLines((ls) => ls.filter((_, idx) => idx !== i))} aria-label="Remove line">
              <Trash2 className="h-4 w-4" />
            </button>
          </div>
        ))}
        <Button variant="ghost" icon={Plus} onClick={() => setLines((ls) => [...ls, blankLine(medicines[0]?.id)])}>
          Add medicine
        </Button>
      </div>
    </Modal>
  );
}
