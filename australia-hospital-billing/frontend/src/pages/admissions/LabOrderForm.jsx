import { useEffect, useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import Modal from '../../components/ui/Modal';
import Button from '../../components/ui/Button';
import Field from '../../components/ui/Field';
import AdmissionSelect from '../../components/AdmissionSelect';
import { labApi } from '../../api';
import useAction from '../../hooks/useAction';
import { money } from '../../utils/format';

export default function LabOrderForm({ open, admission: preset, onClose, onSaved }) {
  const [admission, setAdmission] = useState(preset || null);
  const [tests, setTests] = useState([]);
  const [lines, setLines] = useState([]);
  const { busy, run } = useAction();

  useEffect(() => {
    labApi.tests(true).then((list) => {
      setTests(list);
      setLines(list[0] ? [{ labTestId: list[0].id, quantity: 1 }] : []);
    });
  }, []);

  const update = (i, patch) => setLines((ls) => ls.map((l, idx) => (idx === i ? { ...l, ...patch } : l)));

  const save = async () => {
    const saved = await run(
      () =>
        labApi.createOrder({
          admissionId: admission.id,
          doctorId: admission.doctorId,
          items: lines.map((l) => ({ labTestId: Number(l.labTestId), quantity: Number(l.quantity) })),
        }),
      'Lab order placed',
    );
    if (saved) onSaved(saved);
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Order pathology / imaging"
      size="max-w-xl"
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button loading={busy} disabled={!admission || lines.length === 0 || tests.length === 0} onClick={save}>Place order</Button>
        </>
      }
    >
      <div className="space-y-4">
        {!preset && (
          <Field label="Admission" required>
            <AdmissionSelect value={admission} onChange={setAdmission} />
          </Field>
        )}
        {admission && <p className="text-sm text-slate-500">Ordering doctor: {admission.doctorName}</p>}
        {tests.length === 0 && (
          <p className="rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-800">
            The test catalogue is empty. An administrator or lab technician can add tests under Laboratory &gt; Test catalogue &gt; Add test.
          </p>
        )}
        {lines.map((line, i) => (
          <div key={i} className="flex items-end gap-2">
            <Field label={i === 0 ? 'Test' : ''} className="flex-1">
              <select className="input" value={line.labTestId} onChange={(e) => update(i, { labTestId: e.target.value })}>
                {tests.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.testCode} - {t.testName} ({t.category}) - {money(t.unitPrice)}{t.mbsItemNo ? ` - MBS ${t.mbsItemNo}` : ''}{t.gstFree ? '' : ' + GST'}
                  </option>
                ))}
              </select>
            </Field>
            <Field label={i === 0 ? 'Qty' : ''} className="w-20">
              <input type="number" min={1} max={20} className="input" value={line.quantity} onChange={(e) => update(i, { quantity: e.target.value })} />
            </Field>
            <button type="button" className="mb-2 text-slate-400 hover:text-rose-600" onClick={() => setLines((ls) => ls.filter((_, idx) => idx !== i))} aria-label="Remove line">
              <Trash2 className="h-4 w-4" />
            </button>
          </div>
        ))}
        <Button variant="ghost" icon={Plus} onClick={() => setLines((ls) => [...ls, { labTestId: tests[0]?.id, quantity: 1 }])}>
          Add test
        </Button>
      </div>
    </Modal>
  );
}
