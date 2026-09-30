import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { BedDouble } from 'lucide-react';
import { admissionApi } from '../../api';
import useAsync from '../../hooks/useAsync';
import { useAuth } from '../../auth/AuthContext';
import { can } from '../../auth/roles';
import PageHeader from '../../components/ui/PageHeader';
import Button from '../../components/ui/Button';
import Table from '../../components/ui/Table';
import Pagination from '../../components/ui/Pagination';
import StatusBadge from '../../components/ui/StatusBadge';
import { AsyncContent } from '../../components/ui/States';
import { dateTime, label } from '../../utils/format';
import AdmitForm from './AdmitForm';

export default function AdmissionsPage() {
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const [status, setStatus] = useState('ADMITTED');
  const [page, setPage] = useState(0);
  const [admitting, setAdmitting] = useState(false);
  const state = useAsync(() => admissionApi.search(status, page, 10), [status, page]);

  return (
    <>
      <PageHeader
        title="Admissions"
        subtitle="Inpatient episodes, bed allocation and discharge"
        actions={hasRole(can.admit) && <Button icon={BedDouble} onClick={() => setAdmitting(true)}>Admit patient</Button>}
      />
      <div className="card">
        <div className="flex gap-2 border-b border-slate-100 p-4">
          {['ADMITTED', 'DISCHARGED', 'CANCELLED', ''].map((s) => (
            <button
              key={s || 'ALL'}
              type="button"
              onClick={() => { setStatus(s); setPage(0); }}
              className={`rounded-full px-3 py-1 text-sm font-medium ${status === s ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-600'}`}
            >
              {s ? label(s) : 'All'}
            </button>
          ))}
        </div>
        <AsyncContent state={state} isEmpty={state.data?.content.length === 0} emptyTitle="No admissions">
          {state.data && (
            <>
              <Table
                rows={state.data.content}
                onRowClick={(a) => navigate(`/admissions/${a.id}`)}
                columns={[
                  { key: 'admissionNo', header: 'Admission', render: (a) => <span className="font-mono text-xs">{a.admissionNo}</span> },
                  { key: 'patientName', header: 'Patient', render: (a) => <b>{a.patientName}</b> },
                  { key: 'doctorName', header: 'Doctor' },
                  { key: 'admissionType', header: 'Type' },
                  { key: 'financialClass', header: 'Class', render: (a) => label(a.financialClass) },
                  { key: 'careSetting', header: 'Setting', render: (a) => <StatusBadge status={a.careSetting} /> },
                  { key: 'lengthOfStayDays', header: 'LOS', render: (a) => `${a.lengthOfStayDays} d`, className: 'text-right' },
                  { key: 'admissionDate', header: 'Admitted', render: (a) => dateTime(a.admissionDate) },
                  { key: 'status', header: 'Status', render: (a) => <StatusBadge status={a.status} /> },
                ]}
              />
              <Pagination page={state.data} onChange={setPage} />
            </>
          )}
        </AsyncContent>
      </div>
      {admitting && (
        <AdmitForm open onClose={() => setAdmitting(false)} onSaved={(a) => navigate(`/admissions/${a.admission.id}`)} />
      )}
    </>
  );
}
