import { useState } from 'react';
import { Plus } from 'lucide-react';
import { insuranceApi } from '../api';
import useAsync from '../hooks/useAsync';
import useAction from '../hooks/useAction';
import { useAuth } from '../auth/AuthContext';
import { can } from '../auth/roles';
import PageHeader from '../components/ui/PageHeader';
import Button from '../components/ui/Button';
import Modal from '../components/ui/Modal';
import Field from '../components/ui/Field';
import Table from '../components/ui/Table';
import { AsyncContent } from '../components/ui/States';
import { label, money } from '../utils/format';
import { clean } from '../utils/forms';

const AGREEMENT_HELP = {
  NO_GAP: 'Fund pays everything above the MBS fee - no out-of-pocket for medical items',
  KNOWN_GAP: 'Patient pays at most the known-gap cap per medical item',
  NON_PARTICIPATING: 'Fund pays only 25% of the MBS fee; patient pays the rest above it',
  NOT_APPLICABLE: 'Medicare',
};

export default function PayersPage() {
  const { hasRole } = useAuth();
  const state = useAsync(() => insuranceApi.companies(), []);
  const [form, setForm] = useState(null);
  const { busy, run } = useAction();

  const save = async () => {
    const body = clean({ ...form, knownGapCap: form.knownGapCap === '' ? null : Number(form.knownGapCap) });
    if (await run(() => insuranceApi.createCompany(body), 'Payer added')) {
      setForm(null);
      state.reload();
    }
  };

  return (
    <>
      <PageHeader
        title="Payers"
        subtitle="Medicare and the private funds this hospital has agreements with"
        actions={hasRole(can.managePayers) && (
          <Button icon={Plus} onClick={() => setForm({ companyName: '', payerType: 'PRIVATE_FUND', networkStatus: 'NO_GAP', knownGapCap: '', abn: '', contactPerson: '', phone: '', email: '' })}>
            Add payer
          </Button>
        )}
      />
      <div className="card">
        <AsyncContent state={state}>
          {state.data && (
            <Table
              rows={state.data}
              columns={[
                { key: 'companyName', header: 'Payer', render: (c) => <b>{c.companyName}</b> },
                { key: 'payerType', header: 'Type', render: (c) => label(c.payerType) },
                { key: 'networkStatus', header: 'Agreement', render: (c) => <span title={AGREEMENT_HELP[c.networkStatus]}>{label(c.networkStatus)}</span> },
                { key: 'knownGapCap', header: 'Known-gap cap', render: (c) => (c.knownGapCap ? money(c.knownGapCap) : '-') },
                { key: 'contactPerson', header: 'Contact', render: (c) => c.contactPerson || '-' },
                { key: 'phone', header: 'Phone', render: (c) => c.phone || '-' },
              ]}
            />
          )}
        </AsyncContent>
      </div>
      {form && (
        <Modal open onClose={() => setForm(null)} title="Add payer" footer={<><Button variant="secondary" onClick={() => setForm(null)}>Cancel</Button><Button loading={busy} onClick={save}>Save</Button></>}>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Name" required className="sm:col-span-2"><input className="input" value={form.companyName} onChange={(e) => setForm({ ...form, companyName: e.target.value })} /></Field>
            <Field label="Type">
              <select className="input" value={form.payerType} onChange={(e) => setForm({ ...form, payerType: e.target.value })}>
                <option value="PRIVATE_FUND">Private fund</option>
                <option value="MEDICARE">Medicare</option>
              </select>
            </Field>
            <Field label="Agreement" hint={AGREEMENT_HELP[form.networkStatus]}>
              <select className="input" value={form.networkStatus} onChange={(e) => setForm({ ...form, networkStatus: e.target.value })}>
                {['NO_GAP', 'KNOWN_GAP', 'NON_PARTICIPATING'].map((n) => <option key={n} value={n}>{label(n)}</option>)}
              </select>
            </Field>
            {form.networkStatus === 'KNOWN_GAP' && (
              <Field label="Known-gap cap (AUD)" required><input type="number" className="input" value={form.knownGapCap} onChange={(e) => setForm({ ...form, knownGapCap: e.target.value })} /></Field>
            )}
            <Field label="ABN" hint="11 digits"><input className="input" value={form.abn} onChange={(e) => setForm({ ...form, abn: e.target.value })} /></Field>
            <Field label="Contact person"><input className="input" value={form.contactPerson} onChange={(e) => setForm({ ...form, contactPerson: e.target.value })} /></Field>
            <Field label="Phone"><input className="input" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></Field>
            <Field label="Email"><input className="input" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></Field>
          </div>
        </Modal>
      )}
    </>
  );
}
