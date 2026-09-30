import { useEffect, useState } from 'react';
import Modal from '../../components/ui/Modal';
import Button from '../../components/ui/Button';
import Field from '../../components/ui/Field';
import { insuranceApi, patientApi } from '../../api';
import useAction from '../../hooks/useAction';

const TIERS = ['BASIC', 'BRONZE', 'BRONZE_PLUS', 'SILVER', 'SILVER_PLUS', 'GOLD'];

export default function PolicyForm({ open, patientId, onClose, onSaved }) {
  const [funds, setFunds] = useState([]);
  const [form, setForm] = useState({
    companyId: '', policyNo: '', coverTier: 'SILVER', excessAmount: '500', annualLimit: '50000',
    startDate: new Date().toISOString().slice(0, 10), endDate: '',
  });
  const { busy, run } = useAction();
  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  useEffect(() => {
    insuranceApi.companies().then((list) => {
      const privateFunds = list.filter((c) => c.payerType === 'PRIVATE_FUND');
      setFunds(privateFunds);
      setForm((f) => ({ ...f, companyId: privateFunds[0]?.id ?? '' }));
    });
  }, []);

  const save = async () => {
    const saved = await run(
      () =>
        patientApi.addPolicy(patientId, {
          companyId: Number(form.companyId),
          policyNo: form.policyNo,
          coverTier: form.coverTier,
          excessAmount: Number(form.excessAmount),
          annualLimit: Number(form.annualLimit),
          startDate: form.startDate,
          endDate: form.endDate || null,
        }),
      'Policy added',
    );
    if (saved) onSaved(saved);
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Add private health policy"
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button loading={busy} onClick={save}>Save policy</Button>
        </>
      }
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Fund" required>
          <select className="input" value={form.companyId} onChange={set('companyId')}>
            {funds.map((f) => <option key={f.id} value={f.id}>{f.companyName} ({f.networkStatus.replaceAll('_', ' ').toLowerCase()})</option>)}
          </select>
        </Field>
        <Field label="Policy number" required><input className="input" value={form.policyNo} onChange={set('policyNo')} /></Field>
        <Field label="Cover tier" required>
          <select className="input" value={form.coverTier} onChange={set('coverTier')}>
            {TIERS.map((t) => <option key={t}>{t}</option>)}
          </select>
        </Field>
        <Field label="Excess (AUD)" required><input type="number" min="0" className="input" value={form.excessAmount} onChange={set('excessAmount')} /></Field>
        <Field label="Annual limit (AUD)" required><input type="number" min="0" className="input" value={form.annualLimit} onChange={set('annualLimit')} /></Field>
        <Field label="Start date" required><input type="date" className="input" value={form.startDate} onChange={set('startDate')} /></Field>
        <Field label="End date"><input type="date" className="input" value={form.endDate} onChange={set('endDate')} /></Field>
      </div>
    </Modal>
  );
}
