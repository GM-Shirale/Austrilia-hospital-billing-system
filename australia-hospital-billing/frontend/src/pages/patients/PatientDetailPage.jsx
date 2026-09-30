import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, BedDouble, Pencil, Plus, ShieldPlus, Trash2 } from 'lucide-react';
import { insuranceApi, patientApi } from '../../api';
import useAsync from '../../hooks/useAsync';
import useAction from '../../hooks/useAction';
import { useAuth } from '../../auth/AuthContext';
import { can } from '../../auth/roles';
import PageHeader from '../../components/ui/PageHeader';
import Button from '../../components/ui/Button';
import Modal from '../../components/ui/Modal';
import Field from '../../components/ui/Field';
import StatusBadge from '../../components/ui/StatusBadge';
import Table from '../../components/ui/Table';
import { AsyncContent } from '../../components/ui/States';
import { date, dateTime, label, money } from '../../utils/format';
import { clean } from '../../utils/forms';
import PatientForm from './PatientForm';
import PolicyForm from './PolicyForm';
import AdmitForm from '../admissions/AdmitForm';

function Info({ label: title, value }) {
  return (
    <div>
      <p className="label">{title}</p>
      <p className="text-sm text-slate-800">{value || '-'}</p>
    </div>
  );
}

export default function PatientDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const patient = useAsync(() => patientApi.get(id), [id]);
  const policies = useAsync(() => patientApi.policies(id), [id]);
  const admissions = useAsync(() => patientApi.admissions(id), [id]);
  const [modal, setModal] = useState(null);
  const [contact, setContact] = useState({ name: '', relation: '', phone: '', email: '', primaryContact: false });
  const { busy, run } = useAction();

  const p = patient.data;

  const addContact = async () => {
    const saved = await run(() => patientApi.addContact(id, clean(contact)), 'Contact added');
    if (saved) {
      patient.setData(saved);
      setModal(null);
      setContact({ name: '', relation: '', phone: '', email: '', primaryContact: false });
    }
  };

  const removeContact = async (contactId) => {
    const saved = await run(() => patientApi.removeContact(id, contactId), 'Contact removed');
    if (saved) patient.setData(saved);
  };

  const changePolicyStatus = async (policyId, status) => {
    if (await run(() => insuranceApi.changePolicyStatus(policyId, status), 'Policy updated')) policies.reload();
  };

  return (
    <>
      <Link to="/patients" className="mb-3 inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-800">
        <ArrowLeft className="h-4 w-4" /> Patients
      </Link>
      <AsyncContent state={patient}>
        {p && (
          <>
            <PageHeader
              title={p.fullName}
              subtitle={`${p.mrn} - registered ${dateTime(p.createdAt)}`}
              actions={
                <>
                  {hasRole(can.editPatients) && (
                    <Button variant="secondary" icon={Pencil} onClick={() => setModal('edit')}>Edit</Button>
                  )}
                  {hasRole(can.admit) && p.status === 'ACTIVE' && (
                    <Button icon={BedDouble} onClick={() => setModal('admit')}>Admit</Button>
                  )}
                </>
              }
            />
            <div className="grid gap-6 lg:grid-cols-3">
              <div className="card grid grid-cols-2 gap-4 p-5 lg:col-span-2">
                <Info label="Status" value={<StatusBadge status={p.status} />} />
                <Info label="Date of birth" value={date(p.dob)} />
                <Info label="Gender" value={label(p.gender)} />
                <Info label="Phone" value={p.phone} />
                <Info label="Email" value={p.email} />
                <Info label="Address" value={[p.address, p.suburb, p.state, p.postcode].filter(Boolean).join(', ')} />
                <Info label="Medicare number" value={p.medicareNo} />
                <Info label="Medicare IRN" value={p.medicareIrn} />
              </div>
              <div className="card p-5">
                <div className="flex items-center justify-between">
                  <h2 className="font-bold">Contacts</h2>
                  {hasRole(can.editPatients) && (
                    <Button variant="ghost" icon={Plus} onClick={() => setModal('contact')}>Add</Button>
                  )}
                </div>
                {p.contacts.length === 0 && <p className="mt-3 text-sm text-slate-500">No emergency contacts.</p>}
                <ul className="mt-3 space-y-3">
                  {p.contacts.map((c) => (
                    <li key={c.id} className="flex items-start justify-between rounded-lg bg-slate-50 p-3 text-sm">
                      <div>
                        <p className="font-semibold">
                          {c.name} {c.primaryContact && <span className="text-xs text-blue-600">(primary)</span>}
                        </p>
                        <p className="text-slate-500">{c.relation} - {c.phone}</p>
                      </div>
                      {hasRole(can.editPatients) && (
                        <button type="button" onClick={() => removeContact(c.id)} className="text-slate-400 hover:text-rose-600" aria-label="Remove contact">
                          <Trash2 className="h-4 w-4" />
                        </button>
                      )}
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            <div className="card mt-6">
              <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
                <h2 className="font-bold">Private health insurance</h2>
                {hasRole(can.editPolicies) && (
                  <Button variant="secondary" icon={ShieldPlus} onClick={() => setModal('policy')}>Add policy</Button>
                )}
              </div>
              <AsyncContent state={policies} isEmpty={policies.data?.length === 0} emptyTitle="No private cover" emptyText="Medicare-only patient">
                {policies.data && (
                  <Table
                    rows={policies.data}
                    columns={[
                      { key: 'companyName', header: 'Fund', render: (x) => <b>{x.companyName}</b> },
                      { key: 'policyNo', header: 'Policy' },
                      { key: 'coverTier', header: 'Tier', render: (x) => label(x.coverTier) },
                      { key: 'networkStatus', header: 'Agreement', render: (x) => label(x.networkStatus) },
                      { key: 'excessAmount', header: 'Excess', render: (x) => money(x.excessAmount) },
                      { key: 'remainingLimit', header: 'Limit left', render: (x) => money(x.remainingLimit) },
                      { key: 'period', header: 'Period', render: (x) => `${date(x.startDate)} - ${x.endDate ? date(x.endDate) : 'ongoing'}` },
                      {
                        key: 'status',
                        header: 'Status',
                        render: (x) =>
                          hasRole(can.managePayers) ? (
                            <select className="input py-1" value={x.status} onChange={(e) => changePolicyStatus(x.id, e.target.value)}>
                              {['ACTIVE', 'SUSPENDED', 'LAPSED', 'CANCELLED'].map((s) => <option key={s}>{s}</option>)}
                            </select>
                          ) : (
                            <StatusBadge status={x.status} />
                          ),
                      },
                    ]}
                  />
                )}
              </AsyncContent>
            </div>

            <div className="card mt-6">
              <h2 className="border-b border-slate-100 px-5 py-4 font-bold">Admissions</h2>
              <AsyncContent state={admissions} isEmpty={admissions.data?.length === 0} emptyTitle="No admissions yet">
                {admissions.data && (
                  <Table
                    rows={admissions.data}
                    onRowClick={(a) => navigate(`/admissions/${a.id}`)}
                    columns={[
                      { key: 'admissionNo', header: 'Admission', render: (a) => <span className="font-mono text-xs">{a.admissionNo}</span> },
                      { key: 'admissionDate', header: 'Admitted', render: (a) => dateTime(a.admissionDate) },
                      { key: 'dischargeDate', header: 'Discharged', render: (a) => dateTime(a.dischargeDate) },
                      { key: 'doctorName', header: 'Doctor' },
                      { key: 'financialClass', header: 'Class', render: (a) => label(a.financialClass) },
                      { key: 'status', header: 'Status', render: (a) => <StatusBadge status={a.status} /> },
                    ]}
                  />
                )}
              </AsyncContent>
            </div>
          </>
        )}
      </AsyncContent>

      {modal === 'edit' && p && (
        <PatientForm open patient={p} onClose={() => setModal(null)} onSaved={(saved) => { patient.setData(saved); setModal(null); }} />
      )}
      {modal === 'policy' && (
        <PolicyForm open patientId={id} onClose={() => setModal(null)} onSaved={() => { policies.reload(); setModal(null); }} />
      )}
      {modal === 'admit' && p && (
        <AdmitForm open patient={p} onClose={() => setModal(null)} onSaved={(a) => navigate(`/admissions/${a.admission.id}`)} />
      )}
      <Modal
        open={modal === 'contact'}
        onClose={() => setModal(null)}
        title="Add emergency contact"
        footer={
          <>
            <Button variant="secondary" onClick={() => setModal(null)}>Cancel</Button>
            <Button loading={busy} onClick={addContact}>Add contact</Button>
          </>
        }
      >
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Name" required><input className="input" value={contact.name} onChange={(e) => setContact({ ...contact, name: e.target.value })} /></Field>
          <Field label="Relation" required><input className="input" value={contact.relation} onChange={(e) => setContact({ ...contact, relation: e.target.value })} /></Field>
          <Field label="Phone" required><input className="input" value={contact.phone} onChange={(e) => setContact({ ...contact, phone: e.target.value })} /></Field>
          <Field label="Email"><input className="input" value={contact.email} onChange={(e) => setContact({ ...contact, email: e.target.value })} /></Field>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={contact.primaryContact} onChange={(e) => setContact({ ...contact, primaryContact: e.target.checked })} />
            Primary contact
          </label>
        </div>
      </Modal>
    </>
  );
}
