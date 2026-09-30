import { useEffect, useState } from 'react';
import { CheckCircle2, FlaskConical, Pencil, Plus, RefreshCw, Save, TestTube2 } from 'lucide-react';
import { labApi } from '../api';
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
import Pagination from '../components/ui/Pagination';
import { AsyncContent } from '../components/ui/States';
import { AuditChips, enteredBy } from '../components/AuditChips';
import { dateTime, label, money } from '../utils/format';
import LabOrderForm from './admissions/LabOrderForm';

const TABS = [['queue', 'Incoming queue'], ['orders', 'All orders'], ['catalogue', 'Test catalogue']];
const TEST_TYPES = ['PATHOLOGY', 'IMAGING', 'DIAGNOSTIC'];
const EMPTY_TEST = {
  testCode: '', testName: '', category: 'Biochemistry', testType: 'PATHOLOGY', unitPrice: '', mbsItemNo: '', mbsScheduleFee: '', gstFree: true, active: true,
};

const minutesAgo = (value) => Math.max(0, Math.round((Date.now() - new Date(value)) / 60000));
const waited = (value) => {
  const m = minutesAgo(value);
  return m < 60 ? `${m} min` : m < 1440 ? `${Math.floor(m / 60)} h ${m % 60} min` : `${Math.floor(m / 1440)} d`;
};

/** "Who did what" chips for a lab order. */
const labChips = (order) => {
  const a = order.audit || {};
  return [
    { text: `Ordered: ${order.doctorName}${enteredBy(order.doctorName, a.orderedByName)}` },
    a.collectedByName && { text: `Collected: ${a.collectedByName}, ${dateTime(a.collectedAt)}`, tone: 'blue' },
    a.performedByName && { text: `Performed: ${a.performedByName}, ${dateTime(a.completedAt)}`, tone: 'green' },
    a.cancelledByName && { text: `Cancelled: ${a.cancelledByName}, ${dateTime(a.cancelledAt)}`, tone: 'red' },
  ];
};

export default function LabPage() {
  const { hasRole } = useAuth();
  const [tab, setTab] = useState('queue');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const queue = useAsync(() => labApi.queue(), []);
  const orders = useAsync(() => labApi.orders(status, page, 10), [status, page]);
  const tests = useAsync(() => labApi.tests(), []);
  const [ordering, setOrdering] = useState(false);
  const [detail, setDetail] = useState(null);
  const [results, setResults] = useState({});
  const [notes, setNotes] = useState('');
  const [test, setTest] = useState(null);
  const [testErrors, setTestErrors] = useState({});
  const { busy, run } = useAction();
  const { setData: setQueue } = queue;

  // The queue refreshes itself so new doctor orders appear without reloading the page.
  useEffect(() => {
    const timer = setInterval(() => labApi.queue().then(setQueue).catch(() => {}), 20000);
    return () => clearInterval(timer);
  }, [setQueue]);

  const reloadLists = () => {
    queue.reload();
    orders.reload();
  };

  const openDetail = async (row) => {
    const full = row.items?.length ? row : await run(() => labApi.order(row.id));
    if (full) {
      setDetail(full);
      setNotes(full.audit?.resultNotes || '');
      setResults(Object.fromEntries(full.items.map((i) => [i.id, i.result || ''])));
    }
  };

  const changeStatus = async (next) => {
    const saved = await run(() => labApi.changeStatus(detail.id, next, notes), `Order ${label(next).toLowerCase()}`);
    if (saved) {
      setDetail(saved);
      reloadLists();
    }
  };

  const saveResult = async (itemId) => {
    const saved = await run(() => labApi.recordResult(detail.id, itemId, results[itemId]), 'Result saved');
    if (saved) setDetail(saved);
  };

  const validateTest = (t) => {
    const e = {};
    if (!/^[A-Za-z0-9_-]{2,20}$/.test(t.testCode.trim())) e.testCode = '2-20 letters, digits, - or _';
    if (!t.testName.trim()) e.testName = 'Required';
    if (!t.category.trim()) e.category = 'Required';
    if (t.unitPrice === '' || Number(t.unitPrice) < 0) e.unitPrice = 'Enter the base fee';
    if (t.mbsItemNo && !/^\d{1,5}$/.test(t.mbsItemNo.trim())) e.mbsItemNo = '1-5 digits';
    if (t.mbsItemNo && t.mbsScheduleFee === '') e.mbsScheduleFee = 'Needed for the Medicare rebate';
    if (t.mbsItemNo && !t.gstFree) e.gstFree = 'MBS services are GST-free';
    return e;
  };

  const saveTest = async () => {
    const errors = validateTest(test);
    setTestErrors(errors);
    if (Object.keys(errors).length) return;
    const body = {
      testCode: test.testCode.trim(),
      testName: test.testName.trim(),
      category: test.category.trim(),
      testType: test.testType,
      unitPrice: Number(test.unitPrice),
      mbsItemNo: test.mbsItemNo.trim() || null,
      mbsScheduleFee: test.mbsItemNo.trim() ? Number(test.mbsScheduleFee) : null,
      gstFree: test.gstFree,
      active: test.active,
    };
    const saved = await run(() => (test.id ? labApi.updateTest(test.id, body) : labApi.createTest(body)), test.id ? 'Test updated' : 'Test added to the catalogue');
    if (saved) {
      setTest(null);
      tests.reload();
    }
  };

  const setT = (key) => (e) => setTest((t) => ({ ...t, [key]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }));
  const err = (key) => testErrors[key] && <span className="mt-1 block text-xs text-rose-600">{testErrors[key]}</span>;
  const queueRows = queue.data || [];

  return (
    <>
      <PageHeader
        title="Laboratory"
        subtitle="Pathology, imaging and diagnostics. Every non-cancelled order is billed to the admission with its MBS item (GST-free)."
        actions={hasRole(can.orderLab) && <Button icon={FlaskConical} onClick={() => setOrdering(true)}>New order</Button>}
      />
      <div className="mb-4 flex flex-wrap gap-2">
        {TABS.map(([key, text]) => (
          <button key={key} type="button" onClick={() => setTab(key)} className={`rounded-full px-4 py-1.5 text-sm font-medium ${tab === key ? 'bg-blue-600 text-white' : 'bg-white text-slate-600 ring-1 ring-slate-200'}`}>
            {text}{key === 'queue' && queue.data ? ` (${queueRows.length})` : ''}
          </button>
        ))}
      </div>

      {tab === 'queue' && (
        <div className="card">
          <div className="flex items-center justify-between border-b border-slate-100 p-4">
            <p className="text-sm text-slate-500">Orders placed by doctors that still need collecting or results, oldest first.</p>
            <Button variant="secondary" icon={RefreshCw} onClick={queue.reload}>Refresh</Button>
          </div>
          <AsyncContent state={queue} isEmpty={queueRows.length === 0} emptyTitle="Queue is clear" emptyText="New orders from doctors appear here automatically.">
            <ul className="divide-y divide-slate-100">
              {queueRows.map((o) => (
                <li key={o.id}>
                  <button type="button" onClick={() => openDetail(o)} className="w-full px-5 py-4 text-left hover:bg-blue-50/40">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <span className="flex items-center gap-2">
                        <StatusBadge status={o.status} />
                        <b>{o.patientName}</b>
                        <span className="text-xs text-slate-500">{o.patientMrn} - {o.admissionNo}</span>
                      </span>
                      <span className="text-xs text-slate-500"><span className="font-mono">{o.orderNo}</span> - waiting {waited(o.orderDate)}</span>
                    </div>
                    <p className="mt-1 text-sm text-slate-700">
                      {o.items.map((i) => `${i.testName}${i.mbsItemNo ? ` (MBS ${i.mbsItemNo})` : ''}${i.quantity > 1 ? ` x${i.quantity}` : ''}`).join(', ')}
                    </p>
                    <div className="mt-2"><AuditChips chips={labChips(o)} /></div>
                  </button>
                </li>
              ))}
            </ul>
          </AsyncContent>
        </div>
      )}

      {tab === 'orders' && (
        <div className="card">
          <div className="flex flex-wrap gap-2 border-b border-slate-100 p-4">
            {['', 'ORDERED', 'COLLECTED', 'COMPLETED', 'CANCELLED'].map((s) => (
              <button key={s || 'ALL'} type="button" onClick={() => { setStatus(s); setPage(0); }} className={`rounded-full px-3 py-1 text-sm ${status === s ? 'bg-slate-800 text-white' : 'bg-slate-100 text-slate-600'}`}>
                {s ? label(s) : 'All'}
              </button>
            ))}
          </div>
          <AsyncContent state={orders} isEmpty={orders.data?.content.length === 0} emptyTitle="No lab orders">
            {orders.data && (
              <>
                <Table
                  rows={orders.data.content}
                  onRowClick={openDetail}
                  columns={[
                    { key: 'orderNo', header: 'Order', render: (o) => <span className="font-mono text-xs">{o.orderNo}</span> },
                    { key: 'patientName', header: 'Patient', render: (o) => <b>{o.patientName}</b> },
                    { key: 'admissionNo', header: 'Admission' },
                    { key: 'doctorName', header: 'Ordered by' },
                    { key: 'performed', header: 'Performed by', render: (o) => o.audit?.performedByName || '-' },
                    { key: 'orderDate', header: 'Ordered', render: (o) => dateTime(o.orderDate) },
                    { key: 'status', header: 'Status', render: (o) => <StatusBadge status={o.status} /> },
                  ]}
                />
                <Pagination page={orders.data} onChange={setPage} />
              </>
            )}
          </AsyncContent>
        </div>
      )}

      {tab === 'catalogue' && (
        <div className="card">
          <div className="flex items-center justify-between border-b border-slate-100 p-4">
            <p className="text-sm text-slate-500">MBS-coded pathology and imaging are GST-free; the MBS schedule fee drives the Medicare rebate.</p>
            {hasRole(can.manageLabTests) && (
              <Button icon={Plus} onClick={() => { setTestErrors({}); setTest({ ...EMPTY_TEST }); }}>Add test</Button>
            )}
          </div>
          <AsyncContent state={tests} isEmpty={(tests.data || []).length === 0} emptyTitle="No tests in the catalogue" emptyText="Use Add test to create the first one.">
            {tests.data && (
              <Table
                rows={tests.data}
                onRowClick={hasRole(can.manageLabTests) ? (t) => { setTestErrors({}); setTest({ ...t, mbsItemNo: t.mbsItemNo || '', mbsScheduleFee: t.mbsScheduleFee ?? '' }) } : undefined}
                columns={[
                  { key: 'testCode', header: 'Code', render: (t) => <span className="font-mono text-xs">{t.testCode}</span> },
                  { key: 'testName', header: 'Test', render: (t) => <b className={t.active ? '' : 'text-slate-400 line-through'}>{t.testName}</b> },
                  { key: 'testType', header: 'Type', render: (t) => label(t.testType) },
                  { key: 'category', header: 'Category' },
                  { key: 'unitPrice', header: 'Base fee', render: (t) => money(t.unitPrice), className: 'text-right' },
                  { key: 'mbs', header: 'MBS item / schedule fee', render: (t) => (t.mbsItemNo ? `${t.mbsItemNo} / ${money(t.mbsScheduleFee)}` : '-') },
                  { key: 'gst', header: 'GST', render: (t) => (t.gstFree ? <span className="text-emerald-700">GST-free</span> : '10%') },
                  { key: 'active', header: '', render: (t) => (t.active ? '' : <StatusBadge status="INACTIVE" />) },
                  { key: 'edit', header: '', render: () => hasRole(can.manageLabTests) && <Pencil className="h-4 w-4 text-slate-400" /> },
                ]}
              />
            )}
          </AsyncContent>
        </div>
      )}

      {ordering && <LabOrderForm open onClose={() => setOrdering(false)} onSaved={() => { setOrdering(false); reloadLists(); }} />}

      {detail && (
        <Modal
          open
          size="max-w-2xl"
          onClose={() => setDetail(null)}
          title={`${detail.orderNo} - ${detail.patientName}`}
          footer={
            hasRole(can.processLab) && (
              <>
                {detail.status === 'ORDERED' && <Button icon={TestTube2} loading={busy} onClick={() => changeStatus('COLLECTED')}>Mark collected</Button>}
                {detail.status === 'COLLECTED' && <Button variant="success" icon={CheckCircle2} loading={busy} onClick={() => changeStatus('COMPLETED')}>Complete & sign off</Button>}
                {['ORDERED', 'COLLECTED'].includes(detail.status) && detail.admissionActive && (
                  <Button variant="danger" loading={busy} onClick={() => changeStatus('CANCELLED')}>Cancel order</Button>
                )}
              </>
            )
          }
        >
          <div className="mb-3 space-y-2 text-sm">
            <div className="flex items-center gap-2"><StatusBadge status={detail.status} /><span className="text-slate-500">{detail.admissionNo} - ordered {dateTime(detail.orderDate)}</span></div>
            <AuditChips chips={labChips(detail)} />
          </div>
          <ul className="space-y-3">
            {detail.items.map((item) => (
              <li key={item.id} className="rounded-lg border border-slate-200 p-3">
                <p className="text-sm font-semibold">
                  {item.testName} x {item.quantity}
                  <span className="font-normal text-slate-500"> ({money(item.price)}{item.mbsItemNo ? `, MBS ${item.mbsItemNo}` : ''})</span>
                </p>
                {hasRole(can.processLab) && ['ORDERED', 'COLLECTED'].includes(detail.status) ? (
                  <div className="mt-2 flex gap-2">
                    <input className="input" placeholder="Result" maxLength={500} value={results[item.id] || ''} onChange={(e) => setResults({ ...results, [item.id]: e.target.value })} />
                    <Button variant="secondary" icon={Save} disabled={!results[item.id]} onClick={() => saveResult(item.id)}>Save</Button>
                  </div>
                ) : (
                  <p className="mt-1 text-sm text-slate-600">{item.result || 'No result yet'}</p>
                )}
                {item.resultedByName && <p className="mt-1 text-xs text-slate-400">Result by {item.resultedByName}, {dateTime(item.resultedAt)}</p>}
              </li>
            ))}
          </ul>
          {hasRole(can.processLab) && ['ORDERED', 'COLLECTED'].includes(detail.status) ? (
            <Field label="Result notes / reason (saved when you complete or cancel)" className="mt-4">
              <textarea className="input" rows={2} maxLength={1000} value={notes} onChange={(e) => setNotes(e.target.value)} />
            </Field>
          ) : (
            detail.audit?.resultNotes && <p className="mt-4 rounded-lg bg-slate-50 p-3 text-sm"><b>Notes:</b> {detail.audit.resultNotes}</p>
          )}
        </Modal>
      )}

      {test && (
        <Modal
          open
          size="max-w-xl"
          onClose={() => setTest(null)}
          title={test.id ? `Edit test ${test.testCode}` : 'Add test to the catalogue'}
          footer={<><Button variant="secondary" onClick={() => setTest(null)}>Cancel</Button><Button loading={busy} onClick={saveTest}>Save test</Button></>}
        >
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Code" required hint="e.g. FBC, EUC, CXR">
              <input className="input uppercase" disabled={!!test.id} maxLength={20} value={test.testCode} onChange={setT('testCode')} />{err('testCode')}
            </Field>
            <Field label="Type" required>
              <select className="input" value={test.testType} onChange={setT('testType')}>
                {TEST_TYPES.map((t) => <option key={t} value={t}>{label(t)}</option>)}
              </select>
            </Field>
            <Field label="Test name" required className="sm:col-span-2">
              <input className="input" maxLength={120} value={test.testName} onChange={setT('testName')} />{err('testName')}
            </Field>
            <Field label="Category" required hint="e.g. Haematology, Biochemistry, Imaging">
              <input className="input" maxLength={60} value={test.category} onChange={setT('category')} />{err('category')}
            </Field>
            <Field label="Base fee (AUD)" required>
              <input type="number" min="0" step="0.01" className="input" value={test.unitPrice} onChange={setT('unitPrice')} />{err('unitPrice')}
            </Field>
            <Field label="MBS item" hint="Leave empty if not Medicare-rebatable">
              <input className="input" inputMode="numeric" maxLength={5} value={test.mbsItemNo} onChange={setT('mbsItemNo')} />{err('mbsItemNo')}
            </Field>
            <Field label="MBS schedule fee (AUD)">
              <input type="number" min="0" step="0.01" className="input" disabled={!test.mbsItemNo} value={test.mbsScheduleFee} onChange={setT('mbsScheduleFee')} />{err('mbsScheduleFee')}
            </Field>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={test.gstFree} onChange={setT('gstFree')} /> GST-free medical service
            </label>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={test.active} onChange={setT('active')} /> Active (can be ordered)
            </label>
            {err('gstFree')}
          </div>
          <p className="mt-3 text-xs text-slate-500">Check MBS item numbers and fees against MBS Online. Changing a fee affects new orders only.</p>
        </Modal>
      )}
    </>
  );
}
