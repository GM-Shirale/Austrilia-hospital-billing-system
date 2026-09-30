import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, CheckCircle2, CreditCard, FilePlus2, Lock, Plus, RefreshCcw, Sparkles, Trash2, Undo2, XCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import { aiApi, billingApi, claimApi } from '../../api';
import AiBadge from '../../components/AiBadge';
import MbsSuggester from '../../components/MbsSuggester';
import useAsync from '../../hooks/useAsync';
import useAction from '../../hooks/useAction';
import PageHeader from '../../components/ui/PageHeader';
import Button from '../../components/ui/Button';
import Modal from '../../components/ui/Modal';
import Field from '../../components/ui/Field';
import StatusBadge from '../../components/ui/StatusBadge';
import Table from '../../components/ui/Table';
import { AsyncContent } from '../../components/ui/States';
import { dateTime, label, money } from '../../utils/format';
import InvoiceButton from '../../components/InvoiceButton';
import { invoiceSections } from '../../utils/pdf';

const MANUAL_TYPES = ['SURGERY', 'PROCEDURE', 'CONSULTATION', 'NURSING', 'EQUIPMENT', 'OTHER'];
const PAYMENT_MODES = ['CARD', 'EFTPOS', 'CASH', 'BANK_TRANSFER', 'BPAY'];

function Row({ name, value, strong }) {
  return (
    <div className={`flex justify-between py-1 text-sm ${strong ? 'font-bold text-slate-900' : 'text-slate-600'}`}>
      <span>{name}</span>
      <span>{money(value)}</span>
    </div>
  );
}

export default function BillDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const bill = useAsync(() => billingApi.get(id), [id]);
  const payments = useAsync(() => billingApi.payments(id), [id]);
  const claims = useAsync(() => claimApi.forBill(id), [id]);
  const [modal, setModal] = useState(null);
  const [item, setItem] = useState(null);
  const [payment, setPayment] = useState(null);
  const [refund, setRefund] = useState(null);
  const [explanation, setExplanation] = useState(null);
  const { busy, run } = useAction();
  const b = bill.data;

  const update = async (action, message) => {
    const saved = await run(action, message);
    if (saved) bill.setData(saved);
    return saved;
  };

  const addItem = async () => {
    const body = {
      ...item,
      quantity: Number(item.quantity),
      unitPrice: Number(item.unitPrice),
      discountAmount: item.discountAmount === '' ? null : Number(item.discountAmount),
      mbsScheduleFee: item.mbsScheduleFee === '' ? null : Number(item.mbsScheduleFee),
      mbsItemNo: item.mbsItemNo || null,
    };
    if (await update(() => billingApi.addItem(id, body), 'Line added')) setModal(null);
  };

  const pay = async () => {
    const saved = await run(() => billingApi.pay(id, { ...payment, amount: Number(payment.amount) }), 'Payment recorded');
    if (saved) {
      setModal(null);
      bill.reload();
      payments.reload();
    }
  };

  const doRefund = async () => {
    const saved = await run(() => billingApi.refund(refund.paymentId, { amount: Number(refund.amount), reason: refund.reason }), 'Refund processed');
    if (saved) {
      setModal(null);
      bill.reload();
      payments.reload();
    }
  };

  const explain = async () => {
    const result = await run(() => aiApi.explainBill(id));
    if (result) {
      setExplanation(result);
      setModal('explain');
    }
  };

  // Finalising creates the Medicare / fund claims automatically on the server.
  const finalize = async () => {
    const saved = await update(() => billingApi.finalize(id), 'Bill finalised');
    if (saved) {
      const created = await claimApi.forBill(id).catch(() => []);
      claims.setData(created);
      if (created.length) toast.success(`${created.length} claim(s) created: ${created.map((c) => c.payerName).join(', ')}`);
    }
  };

  const createClaims = async () => {
    const created = await run(() => claimApi.createForBill(id), 'Claims created');
    if (created) {
      claims.reload();
      if (created.length === 1) navigate(`/claims/${created[0].id}`);
    }
  };

  const isDraft = b?.status === 'DRAFT';
  const payable = ['FINALIZED', 'PARTIALLY_PAID'].includes(b?.status);
  const patientOwes = b ? Number(b.patientBalance) : 0;
  const sections = b ? invoiceSections(b.items) : [];

  return (
    <>
      <Link to="/bills" className="mb-3 inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-800">
        <ArrowLeft className="h-4 w-4" /> Bills
      </Link>
      <AsyncContent state={bill}>
        {b && (
          <>
            <PageHeader
              title={`Bill ${b.billNo}`}
              subtitle={`${b.patientName} (${b.patientMrn}) - ${b.admissionNo} (${b.careSetting}, ${b.lengthOfStayDays} day stay) - ${b.doctorName}`}
              actions={
                <>
                  <InvoiceButton billId={b.id} />
                  <Button variant="secondary" icon={Sparkles} loading={busy} onClick={explain}>Explain bill</Button>
                  {isDraft && <Button variant="secondary" icon={RefreshCcw} loading={busy} onClick={() => update(() => billingApi.recalculate(id), 'Charges refreshed')}>Refresh charges</Button>}
                  {isDraft && (
                    <Button variant="secondary" icon={Plus} onClick={() => { setItem({ itemType: 'PROCEDURE', description: '', mbsItemNo: '', mbsScheduleFee: '', quantity: 1, unitPrice: 0, discountAmount: '' }); setModal('item'); }}>
                      Add line
                    </Button>
                  )}
                  {isDraft && <Button icon={CheckCircle2} loading={busy} onClick={finalize}>Finalise & create claims</Button>}
                  {payable && patientOwes > 0 && (
                    <Button variant="success" icon={CreditCard} onClick={() => { setPayment({ amount: patientOwes.toFixed(2), paymentMode: 'CARD', transactionNo: '' }); setModal('pay'); }}>
                      Record payment
                    </Button>
                  )}
                  {['DRAFT', 'FINALIZED'].includes(b.status) && Number(b.paidAmount) === 0 && (
                    <Button variant="ghost" icon={XCircle} onClick={() => update(() => billingApi.cancel(id), 'Bill cancelled')}>Cancel bill</Button>
                  )}
                </>
              }
            />
            {isDraft && (b.warnings || []).length > 0 && (
              <ul className="mb-4 space-y-1 rounded-lg bg-amber-50 px-4 py-2 text-sm text-amber-800">
                {b.warnings.map((w) => <li key={w}>{w}</li>)}
              </ul>
            )}
            {b.chargesLockedAt && (
              <p className="mb-4 flex items-center gap-2 rounded-lg bg-slate-100 px-4 py-2 text-sm text-slate-700">
                <Lock className="h-4 w-4" /> Clinical charges were calculated and locked at discharge ({dateTime(b.chargesLockedAt)}). Only manual adjustments can still be added before finalising.
              </p>
            )}

            <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
              {[['ROOM', 'Room charges'], ['CONSULT', 'Consultations'], ['LAB', 'Lab tests'], ['PHARMACY', 'Pharmacy'], ['OTHER', 'Other']].map(([key, title]) => {
                const s = sections.find((x) => x.key === key);
                return (
                  <div key={key} className="card p-4">
                    <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{title}</p>
                    <p className="mt-1 text-lg font-bold">{money(s?.net ?? 0)}</p>
                    <p className="text-xs text-slate-500">Medicare {money(s?.medicare ?? 0)} - patient {money(s?.patient ?? 0)}</p>
                  </div>
                );
              })}
            </div>

            <div className="grid gap-6 lg:grid-cols-3">
              <div className="card p-5">
                <div className="flex items-center justify-between">
                  <h2 className="font-bold">Totals</h2>
                  <span className="flex gap-1"><StatusBadge status={b.status} /><StatusBadge status={b.paymentStatus} /></span>
                </div>
                <div className="mt-3 divide-y divide-slate-100">
                  <div>
                    <Row name="Gross" value={b.grossAmount} />
                    <Row name="Discount" value={b.discountAmount} />
                    <Row name="GST" value={b.gstAmount} />
                    <Row name="Net total" value={b.netAmount} strong />
                  </div>
                  <div className="pt-1">
                    <Row name="Medicare benefit" value={b.medicareAmount} />
                    <Row name="Insurance benefit" value={b.insuranceAmount} />
                    <Row name="Patient payable" value={b.patientPayableAmount} strong />
                  </div>
                  <div className="pt-1">
                    <Row name="Paid by patient" value={b.patientPaidAmount} />
                    <Row name="Patient balance" value={b.patientBalance} strong />
                  </div>
                  <div className="pt-1">
                    <Row name="Paid (all payers incl. remittances)" value={b.paidAmount} />
                    <Row name="Outstanding (whole bill)" value={b.outstandingAmount} />
                  </div>
                </div>
              </div>
              <div className="card p-5">
                <h2 className="font-bold">Cover used for the estimate</h2>
                <dl className="mt-3 space-y-2 text-sm">
                  <div className="flex justify-between"><dt className="text-slate-500">Patient class</dt><dd>{label(b.coverage.financialClass)}</dd></div>
                  <div className="flex justify-between"><dt className="text-slate-500">Medicare</dt><dd>{b.coverage.medicareEligible ? 'Eligible' : 'Not eligible'}</dd></div>
                  <div className="flex justify-between"><dt className="text-slate-500">Private fund</dt><dd>{b.coverage.fundName || 'None'}</dd></div>
                  {b.coverage.fundName && (
                    <>
                      <div className="flex justify-between"><dt className="text-slate-500">Tier / agreement</dt><dd>{label(b.coverage.coverTier)} / {label(b.coverage.networkStatus)}</dd></div>
                      <div className="flex justify-between"><dt className="text-slate-500">Excess</dt><dd>{money(b.coverage.excessAmount)}</dd></div>
                      <div className="flex justify-between"><dt className="text-slate-500">Annual limit left</dt><dd>{money(b.coverage.remainingAnnualLimit)}</dd></div>
                    </>
                  )}
                </dl>
                {b.adjudicationNotes?.length > 0 && (
                  <ul className="mt-4 list-disc space-y-1 pl-4 text-xs text-slate-500">
                    {b.adjudicationNotes.map((n) => <li key={n}>{n}</li>)}
                  </ul>
                )}
              </div>
              <div className="card p-5">
                <div className="flex items-center justify-between">
                  <h2 className="font-bold">Claims</h2>
                  {payable || b.status === 'PAID' ? (
                    (claims.data || []).length === 0 && (Number(b.medicareAmount) > 0 || Number(b.insuranceAmount) > 0) && (
                      <Button variant="secondary" icon={FilePlus2} loading={busy} onClick={createClaims}>Create claims</Button>
                    )
                  ) : null}
                </div>
                {!payable && b.status !== 'PAID' && <p className="mt-3 text-sm text-slate-500">Finalise the bill to claim Medicare / the fund.</p>}
                <ul className="mt-3 space-y-2">
                  {(claims.data || []).map((c) => (
                    <li key={c.id}>
                      <Link to={`/claims/${c.id}`} className="flex items-center justify-between rounded-lg border border-slate-200 p-3 text-sm hover:bg-slate-50">
                        <span>
                          <b>{c.payerName}</b>
                          <span className="block font-mono text-xs text-slate-500">{c.claimNo}</span>
                        </span>
                        <span className="text-right">
                          <StatusBadge status={c.status} />
                          <span className="block text-xs text-slate-500">{money(c.claimedAmount)}</span>
                        </span>
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            <div className="card mt-6">
              <h2 className="border-b border-slate-100 px-5 py-4 font-bold">Line items</h2>
              <Table
                rows={b.items}
                columns={[
                  { key: 'itemType', header: 'Type', render: (i) => <span className="flex items-center gap-1 text-xs font-semibold text-slate-500">{i.locked && <Lock className="h-3 w-3" aria-label="locked" />}{i.itemType}</span> },
                  { key: 'description', header: 'Description', render: (i) => <>{i.description}{i.mbsItemNo && <span className="block text-xs text-slate-400">MBS {i.mbsItemNo} - schedule {money(i.mbsScheduleFee)}</span>}</> },
                  { key: 'quantity', header: 'Qty', render: (i) => Number(i.quantity), className: 'text-right' },
                  { key: 'unitPrice', header: 'Unit', render: (i) => money(i.unitPrice), className: 'text-right' },
                  { key: 'gstAmount', header: 'GST', render: (i) => money(i.gstAmount), className: 'text-right' },
                  { key: 'netAmount', header: 'Net', render: (i) => <b>{money(i.netAmount)}</b>, className: 'text-right' },
                  { key: 'medicareBenefit', header: 'Medicare', render: (i) => money(i.medicareBenefit), className: 'text-right' },
                  { key: 'insuranceCoveredAmount', header: 'Fund', render: (i) => money(i.insuranceCoveredAmount), className: 'text-right' },
                  { key: 'patientAmount', header: 'Patient', render: (i) => money(i.patientAmount), className: 'text-right' },
                  {
                    key: 'actions',
                    header: '',
                    render: (i) =>
                      isDraft && i.manual && !i.locked && (
                        <button type="button" className="text-slate-400 hover:text-rose-600" onClick={() => update(() => billingApi.removeItem(id, i.id), 'Line removed')} aria-label="Remove line">
                          <Trash2 className="h-4 w-4" />
                        </button>
                      ),
                  },
                ]}
              />
            </div>

            <div className="card mt-6">
              <h2 className="border-b border-slate-100 px-5 py-4 font-bold">Payments</h2>
              <AsyncContent state={payments} isEmpty={payments.data?.length === 0} emptyTitle="No payments yet">
                {payments.data && (
                  <Table
                    rows={payments.data}
                    columns={[
                      { key: 'paymentNo', header: 'Payment', render: (p) => <span className="font-mono text-xs">{p.paymentNo}</span> },
                      { key: 'paymentDate', header: 'Date', render: (p) => dateTime(p.paymentDate) },
                      { key: 'paidBy', header: 'Paid by', render: (p) => label(p.paidBy) },
                      { key: 'paymentMode', header: 'Mode', render: (p) => label(p.paymentMode) },
                      { key: 'amount', header: 'Amount', render: (p) => money(p.amount), className: 'text-right' },
                      { key: 'refundedAmount', header: 'Refunded', render: (p) => money(p.refundedAmount), className: 'text-right' },
                      { key: 'status', header: 'Status', render: (p) => <StatusBadge status={p.status} /> },
                      {
                        key: 'refund',
                        header: '',
                        render: (p) =>
                          p.paidBy === 'PATIENT' && Number(p.amount) > Number(p.refundedAmount) && (
                            <Button variant="ghost" icon={Undo2} onClick={() => { setRefund({ paymentId: p.id, amount: (Number(p.amount) - Number(p.refundedAmount)).toFixed(2), reason: '' }); setModal('refund'); }}>
                              Refund
                            </Button>
                          ),
                      },
                    ]}
                  />
                )}
              </AsyncContent>
            </div>
          </>
        )}
      </AsyncContent>

      {modal === 'item' && item && (
        <Modal open onClose={() => setModal(null)} title="Add manual line" footer={<><Button variant="secondary" onClick={() => setModal(null)}>Cancel</Button><Button loading={busy} onClick={addItem}>Add</Button></>}>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Type">
              <select className="input" value={item.itemType} onChange={(e) => setItem({ ...item, itemType: e.target.value })}>
                {MANUAL_TYPES.map((t) => <option key={t}>{t}</option>)}
              </select>
            </Field>
            <Field label="Description" required><input className="input" value={item.description} onChange={(e) => setItem({ ...item, description: e.target.value })} /></Field>
            <Field label="MBS item no" hint="Medical items only (e.g. 30445)"><input className="input" value={item.mbsItemNo} onChange={(e) => setItem({ ...item, mbsItemNo: e.target.value })} /></Field>
            <Field label="MBS schedule fee"><input type="number" className="input" value={item.mbsScheduleFee} onChange={(e) => setItem({ ...item, mbsScheduleFee: e.target.value })} /></Field>
            <Field label="Quantity" required><input type="number" min="0.01" step="0.01" className="input" value={item.quantity} onChange={(e) => setItem({ ...item, quantity: e.target.value })} /></Field>
            <Field label="Unit price (AUD)" required><input type="number" min="0" step="0.01" className="input" value={item.unitPrice} onChange={(e) => setItem({ ...item, unitPrice: e.target.value })} /></Field>
            <Field label="Discount (AUD)"><input type="number" min="0" step="0.01" className="input" value={item.discountAmount} onChange={(e) => setItem({ ...item, discountAmount: e.target.value })} /></Field>
          </div>
          <p className="mt-3 text-xs text-slate-500">Tip: the mock fund rejects EQUIPMENT lines, so adding one lets you see a partially rejected claim.</p>
          <div className="mt-4">
            <MbsSuggester
              onPick={(s) =>
                setItem({
                  ...item,
                  itemType: MANUAL_TYPES.includes(s.itemType) ? s.itemType : 'PROCEDURE',
                  description: s.description,
                  mbsItemNo: s.mbsItemNo,
                  mbsScheduleFee: s.indicativeScheduleFee ?? '',
                  unitPrice: s.indicativeScheduleFee ?? item.unitPrice,
                })
              }
            />
          </div>
        </Modal>
      )}
      {modal === 'explain' && explanation && (
        <Modal open onClose={() => setModal(null)} title="Your bill explained" size="max-w-xl" footer={<Button onClick={() => setModal(null)}>Close</Button>}>
          <div className="space-y-3 text-sm">
            <AiBadge source={explanation.source} model={explanation.model} />
            <p className="text-slate-700">{explanation.summary}</p>
            <ul className="list-disc space-y-1 pl-5 text-slate-700">
              {explanation.keyPoints.map((p) => <li key={p}>{p}</li>)}
            </ul>
            <p className="rounded-lg bg-amber-50 p-3 font-semibold text-amber-800">{explanation.whatYouOwe}</p>
            <p className="text-xs text-slate-400">{explanation.disclaimer}</p>
          </div>
        </Modal>
      )}
      {modal === 'pay' && payment && (
        <Modal open onClose={() => setModal(null)} title="Record patient payment" footer={<><Button variant="secondary" onClick={() => setModal(null)}>Cancel</Button><Button variant="success" loading={busy} onClick={pay}>Record</Button></>}>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Amount (AUD)" hint={`Patient owes ${money(patientOwes)}`}><input type="number" step="0.01" className="input" value={payment.amount} onChange={(e) => setPayment({ ...payment, amount: e.target.value })} /></Field>
            <Field label="Mode">
              <select className="input" value={payment.paymentMode} onChange={(e) => setPayment({ ...payment, paymentMode: e.target.value })}>
                {PAYMENT_MODES.map((m) => <option key={m} value={m}>{label(m)}</option>)}
              </select>
            </Field>
            <Field label="Transaction reference" className="sm:col-span-2"><input className="input" value={payment.transactionNo} onChange={(e) => setPayment({ ...payment, transactionNo: e.target.value })} /></Field>
          </div>
        </Modal>
      )}
      {modal === 'refund' && refund && (
        <Modal open onClose={() => setModal(null)} title="Refund payment" footer={<><Button variant="secondary" onClick={() => setModal(null)}>Cancel</Button><Button variant="danger" loading={busy} onClick={doRefund}>Refund</Button></>}>
          <div className="grid gap-4">
            <Field label="Amount (AUD)"><input type="number" step="0.01" className="input" value={refund.amount} onChange={(e) => setRefund({ ...refund, amount: e.target.value })} /></Field>
            <Field label="Reason" required><input className="input" value={refund.reason} onChange={(e) => setRefund({ ...refund, reason: e.target.value })} /></Field>
          </div>
        </Modal>
      )}
    </>
  );
}
