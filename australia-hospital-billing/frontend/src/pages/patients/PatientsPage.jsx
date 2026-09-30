import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, UserPlus } from 'lucide-react';
import { patientApi } from '../../api';
import useAsync from '../../hooks/useAsync';
import useDebounce from '../../hooks/useDebounce';
import { useAuth } from '../../auth/AuthContext';
import { can } from '../../auth/roles';
import PageHeader from '../../components/ui/PageHeader';
import Button from '../../components/ui/Button';
import Table from '../../components/ui/Table';
import Pagination from '../../components/ui/Pagination';
import StatusBadge from '../../components/ui/StatusBadge';
import { AsyncContent } from '../../components/ui/States';
import { date } from '../../utils/format';
import PatientForm from './PatientForm';

export default function PatientsPage() {
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const [term, setTerm] = useState('');
  const [page, setPage] = useState(0);
  const [showForm, setShowForm] = useState(false);
  const debounced = useDebounce(term);
  const state = useAsync(() => patientApi.search(debounced, page, 10), [debounced, page]);

  const columns = [
    { key: 'mrn', header: 'MRN', render: (p) => <span className="font-mono text-xs">{p.mrn}</span> },
    { key: 'fullName', header: 'Name', render: (p) => <span className="font-semibold">{p.fullName}</span> },
    { key: 'dob', header: 'Date of birth', render: (p) => date(p.dob) },
    { key: 'gender', header: 'Gender' },
    { key: 'phone', header: 'Phone', render: (p) => p.phone || '-' },
    { key: 'medicareNo', header: 'Medicare', render: (p) => p.medicareNo || <span className="text-slate-400">none</span> },
    { key: 'status', header: 'Status', render: (p) => <StatusBadge status={p.status} /> },
  ];

  return (
    <>
      <PageHeader
        title="Patients"
        subtitle="Search by name, MRN, Medicare number or phone"
        actions={hasRole(can.editPatients) && <Button icon={UserPlus} onClick={() => setShowForm(true)}>Register patient</Button>}
      />
      <div className="card">
        <div className="border-b border-slate-100 p-4">
          <div className="relative max-w-md">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <input
              className="input pl-9"
              placeholder="Search patients..."
              value={term}
              onChange={(e) => {
                setTerm(e.target.value);
                setPage(0);
              }}
            />
          </div>
        </div>
        <AsyncContent state={state} isEmpty={state.data?.content.length === 0} emptyTitle="No patients found">
          {state.data && (
            <>
              <Table columns={columns} rows={state.data.content} onRowClick={(p) => navigate(`/patients/${p.id}`)} />
              <Pagination page={state.data} onChange={setPage} />
            </>
          )}
        </AsyncContent>
      </div>
      {showForm && (
        <PatientForm
          open
          onClose={() => setShowForm(false)}
          onSaved={(p) => {
            setShowForm(false);
            navigate(`/patients/${p.id}`);
          }}
        />
      )}
    </>
  );
}
