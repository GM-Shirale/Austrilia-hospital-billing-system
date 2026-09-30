import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { billingApi } from '../../api';
import useAsync from '../../hooks/useAsync';
import PageHeader from '../../components/ui/PageHeader';
import Table from '../../components/ui/Table';
import Pagination from '../../components/ui/Pagination';
import StatusBadge from '../../components/ui/StatusBadge';
import { AsyncContent } from '../../components/ui/States';
import { dateTime, label, money } from '../../utils/format';

export default function BillsPage() {
  const navigate = useNavigate();
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const state = useAsync(() => billingApi.search(status, page, 10), [status, page]);

  return (
    <>
      <PageHeader title="Billing" subtitle="A bill is created and locked automatically when a patient is discharged; interim bills can be generated from the admission page" />
      <div className="card">
        <div className="flex flex-wrap gap-2 border-b border-slate-100 p-4">
          {['', 'DRAFT', 'FINALIZED', 'PARTIALLY_PAID', 'PAID', 'CANCELLED'].map((s) => (
            <button key={s || 'ALL'} type="button" onClick={() => { setStatus(s); setPage(0); }} className={`rounded-full px-3 py-1 text-sm ${status === s ? 'bg-slate-800 text-white' : 'bg-slate-100 text-slate-600'}`}>
              {s ? label(s) : 'All'}
            </button>
          ))}
        </div>
        <AsyncContent state={state} isEmpty={state.data?.content.length === 0} emptyTitle="No bills" emptyText="Discharge a patient and generate a bill from the admission page.">
          {state.data && (
            <>
              <Table
                rows={state.data.content}
                onRowClick={(b) => navigate(`/bills/${b.id}`)}
                columns={[
                  { key: 'billNo', header: 'Bill', render: (b) => <span className="font-mono text-xs">{b.billNo}</span> },
                  { key: 'patientName', header: 'Patient', render: (b) => <b>{b.patientName}</b> },
                  { key: 'admissionNo', header: 'Admission' },
                  { key: 'billDate', header: 'Date', render: (b) => dateTime(b.billDate) },
                  { key: 'netAmount', header: 'Net', render: (b) => money(b.netAmount), className: 'text-right' },
                  { key: 'patientPayableAmount', header: 'Patient pays', render: (b) => money(b.patientPayableAmount), className: 'text-right' },
                  { key: 'patientBalance', header: 'Balance', render: (b) => money(b.patientBalance), className: 'text-right' },
                  { key: 'status', header: 'Status', render: (b) => <StatusBadge status={b.status} /> },
                  { key: 'paymentStatus', header: 'Payment', render: (b) => <StatusBadge status={b.paymentStatus} /> },
                ]}
              />
              <Pagination page={state.data} onChange={setPage} />
            </>
          )}
        </AsyncContent>
      </div>
    </>
  );
}
