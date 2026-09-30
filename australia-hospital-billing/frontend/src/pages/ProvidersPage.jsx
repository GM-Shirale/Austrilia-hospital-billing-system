import { useState } from 'react';
import { Building, Plus, UserPlus } from 'lucide-react';
import { providerApi } from '../api';
import useAsync from '../hooks/useAsync';
import useAction from '../hooks/useAction';
import { useAuth } from '../auth/AuthContext';
import { can } from '../auth/roles';
import PageHeader from '../components/ui/PageHeader';
import Button from '../components/ui/Button';
import Modal from '../components/ui/Modal';
import Field from '../components/ui/Field';
import StatusBadge from '../components/ui/StatusBadge';
import Table from '../components/ui/Table';
import { AsyncContent } from '../components/ui/States';
import { money } from '../utils/format';
import { clean } from '../utils/forms';

const EMPTY_DOCTOR = {
  departmentId: '', firstName: '', lastName: '', specialization: '', phone: '', email: '',
  providerNo: '', consultationFee: 200, mbsItemNo: '104', mbsScheduleFee: 98.95, active: true,
};

export default function ProvidersPage() {
  const { hasRole } = useAuth();
  const departments = useAsync(() => providerApi.departments(), []);
  const [departmentId, setDepartmentId] = useState('');
  const doctors = useAsync(() => providerApi.doctors(departmentId || undefined, 0, 100), [departmentId]);
  const [doctor, setDoctor] = useState(null);
  const [dept, setDept] = useState(null);
  const { busy, run } = useAction();

  const saveDoctor = async () => {
    const body = clean({
      ...doctor,
      departmentId: Number(doctor.departmentId),
      consultationFee: Number(doctor.consultationFee),
      mbsScheduleFee: Number(doctor.mbsScheduleFee),
    });
    const saved = await run(() => (doctor.id ? providerApi.updateDoctor(doctor.id, body) : providerApi.createDoctor(body)), 'Doctor saved');
    if (saved) {
      setDoctor(null);
      doctors.reload();
    }
  };

  const saveDepartment = async () => {
    if (await run(() => providerApi.createDepartment(clean(dept)), 'Department created')) {
      setDept(null);
      departments.reload();
    }
  };

  const set = (key) => (e) => setDoctor((d) => ({ ...d, [key]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }));

  return (
    <>
      <PageHeader
        title="Doctors & departments"
        subtitle="Medicare provider numbers and MBS items used for claims"
        actions={hasRole(can.manageProviders) && (
          <>
            <Button variant="secondary" icon={Building} onClick={() => setDept({ departmentName: '', location: '' })}>New department</Button>
            <Button icon={UserPlus} onClick={() => setDoctor({ ...EMPTY_DOCTOR, departmentId: departments.data?.[0]?.id ?? '' })}>New doctor</Button>
          </>
        )}
      />
      <div className="grid gap-6 lg:grid-cols-4">
        <div className="card p-4">
          <h2 className="mb-3 font-bold">Departments</h2>
          <AsyncContent state={departments}>
            <ul className="space-y-1">
              <li>
                <button type="button" onClick={() => setDepartmentId('')} className={`w-full rounded-lg px-3 py-2 text-left text-sm ${departmentId === '' ? 'bg-blue-50 font-semibold text-blue-700' : 'hover:bg-slate-50'}`}>
                  All departments
                </button>
              </li>
              {(departments.data || []).map((d) => (
                <li key={d.id}>
                  <button type="button" onClick={() => setDepartmentId(d.id)} className={`w-full rounded-lg px-3 py-2 text-left text-sm ${departmentId === d.id ? 'bg-blue-50 font-semibold text-blue-700' : 'hover:bg-slate-50'}`}>
                    {d.departmentName}
                    <span className="block text-xs text-slate-400">{d.location}</span>
                  </button>
                </li>
              ))}
            </ul>
          </AsyncContent>
        </div>
        <div className="card lg:col-span-3">
          <AsyncContent state={doctors} isEmpty={doctors.data?.content.length === 0} emptyTitle="No doctors">
            {doctors.data && (
              <Table
                rows={doctors.data.content}
                onRowClick={hasRole(can.manageProviders) ? (d) => setDoctor({ ...EMPTY_DOCTOR, ...d }) : undefined}
                columns={[
                  { key: 'displayName', header: 'Doctor', render: (d) => <><b>{d.displayName}</b><span className="block text-xs text-slate-500">{d.specialization}</span></> },
                  { key: 'departmentName', header: 'Department' },
                  { key: 'providerNo', header: 'Provider no', render: (d) => <span className="font-mono text-xs">{d.providerNo}</span> },
                  { key: 'fee', header: 'Consult fee', render: (d) => money(d.consultationFee) },
                  { key: 'mbs', header: 'MBS item / schedule fee', render: (d) => `${d.mbsItemNo} / ${money(d.mbsScheduleFee)}` },
                  { key: 'active', header: 'Status', render: (d) => <StatusBadge status={d.active ? 'ACTIVE' : 'INACTIVE'} /> },
                ]}
              />
            )}
          </AsyncContent>
        </div>
      </div>

      {doctor && (
        <Modal
          open
          size="max-w-2xl"
          onClose={() => setDoctor(null)}
          title={doctor.id ? `Edit ${doctor.displayName}` : 'New doctor'}
          footer={<><Button variant="secondary" onClick={() => setDoctor(null)}>Cancel</Button><Button loading={busy} onClick={saveDoctor}>Save</Button></>}
        >
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="First name" required><input className="input" value={doctor.firstName} onChange={set('firstName')} /></Field>
            <Field label="Last name" required><input className="input" value={doctor.lastName} onChange={set('lastName')} /></Field>
            <Field label="Department" required>
              <select className="input" value={doctor.departmentId} onChange={set('departmentId')}>
                {(departments.data || []).map((d) => <option key={d.id} value={d.id}>{d.departmentName}</option>)}
              </select>
            </Field>
            <Field label="Specialisation" required><input className="input" value={doctor.specialization} onChange={set('specialization')} /></Field>
            <Field label="Phone"><input className="input" value={doctor.phone || ''} onChange={set('phone')} /></Field>
            <Field label="Email"><input className="input" value={doctor.email || ''} onChange={set('email')} /></Field>
            <Field label="Medicare provider no" required hint="6 digits + location char + check letter, e.g. 2451731J"><input className="input" value={doctor.providerNo} onChange={set('providerNo')} /></Field>
            <Field label="Consultation fee (AUD)" required><input type="number" className="input" value={doctor.consultationFee} onChange={set('consultationFee')} /></Field>
            <Field label="MBS item no" required><input className="input" value={doctor.mbsItemNo} onChange={set('mbsItemNo')} /></Field>
            <Field label="MBS schedule fee (AUD)" required><input type="number" className="input" value={doctor.mbsScheduleFee} onChange={set('mbsScheduleFee')} /></Field>
            <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={doctor.active} onChange={set('active')} /> Active</label>
          </div>
        </Modal>
      )}
      {dept && (
        <Modal
          open
          onClose={() => setDept(null)}
          title="New department"
          footer={<><Button variant="secondary" onClick={() => setDept(null)}>Cancel</Button><Button loading={busy} icon={Plus} onClick={saveDepartment}>Create</Button></>}
        >
          <div className="grid gap-4">
            <Field label="Name" required><input className="input" value={dept.departmentName} onChange={(e) => setDept({ ...dept, departmentName: e.target.value })} /></Field>
            <Field label="Location"><input className="input" value={dept.location} onChange={(e) => setDept({ ...dept, location: e.target.value })} /></Field>
          </div>
        </Modal>
      )}
    </>
  );
}
