import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { claimApi } from '../../api';
import useAsync from '../../hooks/useAsync';
import PageHeader from '../../components/ui/PageHeader';
import Table from '../../components/ui/Table';
import Pagination from '../../components/ui/Pagination';
import StatusBadge from '../../components/ui/StatusBadge';
import { AsyncContent } from '../../components/ui/States';
import { dateTime, label, money } from '../../utils/format';

const FILTERS = ['', 'DRAFT', 'VALIDATED', 'SUBMISSION_QUEUED', 'ACCEPTED', 'REJECTED', 'RECONCILED', 'CLOSED'];

export default function ClaimsPage() {
  const navigate = useNavigate();
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const state = useAsync(() => claimApi.search(status, page, 10), [status, page]);

  return (
    <>
      <PageHeader title="Insurance claims" subtitle="Medicare (ECLIPSE) and private fund claims with full lifecycle tracking" />
      <div className="card">
        <div className="flex flex-wrap gap-2 border-b border-slate-100 p-4">
          {FILTERS.map((s) => (
            <button key={s || 'ALL'} type="button" onClick={() => { setStatus(s); setPage(0); }} className={`rounded-full px-3 py-1 text-sm ${status === s ? 'bg-slate-800 text-white' : 'bg-slate-100 text-slate-600'}`}>
              {s ? label(s) : 'All'}
            </button>
          ))}
        </div>
        <AsyncContent state={state} isEmpty={state.data?.content.length === 0} emptyTitle="No claims" emptyText="Claims are created automatically when a bill is finalised (Billing > open bill > Finalise and create claims): one Medicare (ECLIPSE) claim and one health fund claim; the rest is the patient gap.">
          {state.data && (
            <>
              <Table
                rows={state.data.content}
                onRowClick={(c) => navigate(`/claims/${c.id}`)}
                columns={[
                  { key: 'claimNo', header: 'Claim', render: (c) => <span className="font-mono text-xs">{c.claimNo}</span> },
                  { key: 'payerName', header: 'Payer', render: (c) => <b>{c.payerName}</b> },
                  { key: 'patientName', header: 'Patient' },
                  { key: 'billNo', header: 'Bill' },
                  { key: 'claimDate', header: 'Created', render: (c) => dateTime(c.claimDate) },
                  { key: 'claimedAmount', header: 'Claimed', render: (c) => money(c.claimedAmount), className: 'text-right' },
                  { key: 'paidAmount', header: 'Paid', render: (c) => money(c.paidAmount), className: 'text-right' },
                  { key: 'status', header: 'Status', render: (c) => <StatusBadge status={c.status} /> },
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
