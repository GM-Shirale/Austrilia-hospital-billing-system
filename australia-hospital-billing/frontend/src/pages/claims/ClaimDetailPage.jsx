import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, CheckCircle2, Loader2, Send, Sparkles, XCircle } from 'lucide-react';
import { aiApi, claimApi } from '../../api';
import AiBadge from '../../components/AiBadge';
import useAsync from '../../hooks/useAsync';
import useAction from '../../hooks/useAction';
import PageHeader from '../../components/ui/PageHeader';
import Button from '../../components/ui/Button';
import StatusBadge from '../../components/ui/StatusBadge';
import Table from '../../components/ui/Table';
import { AsyncContent } from '../../components/ui/States';
import { dateTime, label, money } from '../../utils/format';

/** States in which the backend is still working asynchronously (event-driven). */
const IN_FLIGHT = ['SUBMISSION_QUEUED', 'SUBMITTED', 'ACKNOWLEDGED', 'ACCEPTED', 'PAID'];

export default function ClaimDetailPage() {
  const { id } = useParams();
  const state = useAsync(() => claimApi.get(id), [id]);
  const { busy, run } = useAction();
  const [review, setReview] = useState(null);
  const data = state.data;
  const c = data?.claim;
  const inFlight = c && IN_FLIGHT.includes(c.status);

  // Poll while the claim moves through the async pipeline (Kafka / in-memory events).
  useEffect(() => {
    if (!inFlight) return undefined;
    const timer = setInterval(() => claimApi.get(id).then(state.setData).catch(() => {}), 1500);
    return () => clearInterval(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [inFlight, id]);

  const runReview = async () => {
    const result = await run(() => aiApi.reviewClaim(id));
    if (result) setReview(result);
  };

  const act = async (action, message) => {
    const saved = await run(action, message);
    if (saved) state.setData(saved);
  };

  return (
    <>
      <Link to="/claims" className="mb-3 inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-800">
        <ArrowLeft className="h-4 w-4" /> Claims
      </Link>
      <AsyncContent state={state}>
        {c && (
          <>
            <PageHeader
              title={`${c.payerName} claim ${c.claimNo}`}
              subtitle={`${c.patientName} - bill ${c.billNo} - admission ${c.admissionNo}`}
              actions={
                <>
                  <Button variant="secondary" icon={Sparkles} loading={busy} onClick={runReview}>Risk review</Button>
                  {c.status === 'DRAFT' && <Button icon={CheckCircle2} loading={busy} onClick={() => act(() => claimApi.validate(id), 'All checks passed')}>Validate</Button>}
                  {c.status === 'VALIDATED' && <Button icon={Send} loading={busy} onClick={() => act(() => claimApi.submit(id), 'Queued for submission')}>Submit to payer</Button>}
                  {data.allowedNextStatuses.includes('CLOSED') && (
                    <Button variant="secondary" icon={XCircle} loading={busy} onClick={() => act(() => claimApi.close(id, null), 'Claim closed')}>Close</Button>
                  )}
                </>
              }
            />
            {inFlight && (
              <p className="mb-4 flex items-center gap-2 rounded-lg bg-violet-50 px-4 py-2 text-sm text-violet-800">
                <Loader2 className="h-4 w-4 animate-spin" /> Processing asynchronously: lodgement, payer assessment, remittance and EOB. This page updates by itself.
              </p>
            )}
            {review && (
              <div className={`card mb-6 border-l-4 p-5 ${review.riskLevel === 'HIGH' ? 'border-l-rose-500' : review.riskLevel === 'MEDIUM' ? 'border-l-amber-500' : 'border-l-emerald-500'}`}>
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="font-bold">Pre-submission risk: {review.riskLevel}</h2>
                  <AiBadge source={review.source} model={review.model} />
                </div>
                <p className="mt-2 text-sm text-slate-700">{review.summary}</p>
                <div className="mt-3 grid gap-4 sm:grid-cols-2">
                  <div>
                    <p className="label">Issues</p>
                    {review.issues.length === 0 ? <p className="text-sm text-slate-500">None</p> : (
                      <ul className="list-disc space-y-1 pl-5 text-sm text-slate-700">{review.issues.map((i) => <li key={i}>{i}</li>)}</ul>
                    )}
                  </div>
                  <div>
                    <p className="label">Recommendations</p>
                    <ul className="list-disc space-y-1 pl-5 text-sm text-slate-700">{review.recommendations.map((r) => <li key={r}>{r}</li>)}</ul>
                  </div>
                </div>
                <p className="mt-3 text-xs text-slate-400">{review.disclaimer}</p>
              </div>
            )}
            <div className="grid gap-6 lg:grid-cols-3">
              <div className="card space-y-2 p-5 text-sm">
                <div className="flex justify-between"><span className="text-slate-500">Status</span><StatusBadge status={c.status} /></div>
                <div className="flex justify-between"><span className="text-slate-500">Payer type</span><span>{label(c.payerType)}</span></div>
                {data.policyNo && <div className="flex justify-between"><span className="text-slate-500">Policy</span><span>{data.policyNo}</span></div>}
                <div className="flex justify-between"><span className="text-slate-500">Payer reference</span><span className="font-mono">{data.payerReference || '-'}</span></div>
                <div className="flex justify-between"><span className="text-slate-500">Claimed</span><b>{money(c.claimedAmount)}</b></div>
                <div className="flex justify-between"><span className="text-slate-500">Approved</span><span>{money(c.approvedAmount)}</span></div>
                <div className="flex justify-between"><span className="text-slate-500">Rejected</span><span className={Number(c.rejectedAmount) > 0 ? 'text-rose-600' : ''}>{money(c.rejectedAmount)}</span></div>
                <div className="flex justify-between"><span className="text-slate-500">Paid</span><b className="text-emerald-700">{money(c.paidAmount)}</b></div>
                {data.rejectionReason && <p className="rounded-lg bg-rose-50 p-2 text-rose-700">{data.rejectionReason}</p>}
              </div>
              <div className="card p-5 lg:col-span-2">
                <h2 className="font-bold">Status history</h2>
                <ol className="mt-4 space-y-4 border-l-2 border-slate-100 pl-5">
                  {data.history.map((h, i) => (
                    <li key={i} className="relative">
                      <span className="absolute -left-[27px] top-1 h-3 w-3 rounded-full bg-blue-500 ring-4 ring-white" />
                      <div className="flex flex-wrap items-center gap-2">
                        <StatusBadge status={h.toStatus} />
                        <span className="text-xs text-slate-400">{dateTime(h.changedAt)}</span>
                      </div>
                      {h.note && <p className="mt-1 text-sm text-slate-600">{h.note}</p>}
                    </li>
                  ))}
                </ol>
              </div>
            </div>

            <div className="card mt-6">
              <h2 className="border-b border-slate-100 px-5 py-4 font-bold">Claim lines</h2>
              <Table
                rows={data.items}
                columns={[
                  { key: 'itemType', header: 'Type', render: (i) => <span className="text-xs font-semibold text-slate-500">{i.itemType}</span> },
                  { key: 'description', header: 'Description' },
                  { key: 'mbsItemNo', header: 'MBS', render: (i) => i.mbsItemNo || '-' },
                  { key: 'claimedAmount', header: 'Claimed', render: (i) => money(i.claimedAmount), className: 'text-right' },
                  { key: 'approvedAmount', header: 'Approved', render: (i) => money(i.approvedAmount), className: 'text-right' },
                  { key: 'rejectedAmount', header: 'Rejected', render: (i) => money(i.rejectedAmount), className: 'text-right' },
                  { key: 'rejectionReason', header: 'Reason', render: (i) => <span className="text-xs text-rose-600">{i.rejectionReason || ''}</span> },
                ]}
              />
            </div>

            {data.eob && (
              <div className="card mt-6 p-5">
                <h2 className="font-bold">Explanation of Benefits {data.eob.eobNo}</h2>
                <p className="mt-1 text-xs text-slate-400">Issued {dateTime(data.eob.issuedAt)}</p>
                <div className="mt-4 grid gap-4 sm:grid-cols-3">
                  <div className="rounded-xl bg-slate-50 p-4"><p className="label">Charged</p><p className="text-xl font-bold">{money(data.eob.totalCharged)}</p></div>
                  <div className="rounded-xl bg-emerald-50 p-4"><p className="label">Benefit paid</p><p className="text-xl font-bold text-emerald-700">{money(data.eob.totalBenefit)}</p></div>
                  <div className="rounded-xl bg-amber-50 p-4"><p className="label">Patient responsibility</p><p className="text-xl font-bold text-amber-700">{money(data.eob.patientResponsibility)}</p></div>
                </div>
                <p className="mt-4 text-sm text-slate-600">{data.eob.summary}</p>
              </div>
            )}
          </>
        )}
      </AsyncContent>
    </>
  );
}
