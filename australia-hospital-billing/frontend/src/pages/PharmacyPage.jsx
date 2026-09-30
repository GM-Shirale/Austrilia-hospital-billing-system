import { useEffect, useState } from 'react';
import { AlertTriangle, History, PackagePlus, Pencil, Pill, RefreshCw } from 'lucide-react';
import { pharmacyApi } from '../api';
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
import PrescriptionForm from './admissions/PrescriptionForm';

const TABS = [['queue', 'Incoming prescriptions'], ['prescriptions', 'All prescriptions'], ['medicines', 'Formulary & stock']];

/** GST treatment per supply category (A New Tax System (Goods and Services Tax) Act 1999). */
const SUPPLY_CATEGORIES = [
  { value: 'PBS', label: 'PBS-listed medicine', gst: 'GST-free' },
  { value: 'PRESCRIPTION', label: 'Prescription-only (non-PBS)', gst: 'GST-free' },
  { value: 'OTC', label: 'Over-the-counter product', gst: '10% GST' },
  { value: 'TAXABLE_SUPPLY', label: 'Other taxable goods', gst: '10% GST' },
];
const supplyLabel = (v) => SUPPLY_CATEGORIES.find((c) => c.value === v)?.label ?? v;

const rxChips = (p) => {
  const a = p.audit || {};
  return [
    { text: `Prescribed: ${p.doctorName}${enteredBy(p.doctorName, a.prescribedByName)}` },
    a.dispensedByName && { text: `Dispensed: ${a.dispensedByName}, ${dateTime(a.dispensedAt)}`, tone: 'green' },
    a.cancelledByName && { text: `Cancelled: ${a.cancelledByName}, ${dateTime(a.cancelledAt)}`, tone: 'red' },
  ];
};

export default function PharmacyPage() {
  const { hasRole } = useAuth();
  const [tab, setTab] = useState('queue');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const queue = useAsync(() => pharmacyApi.queue(), []);
  const prescriptions = useAsync(() => pharmacyApi.prescriptions(status, page, 10), [status, page]);
  const medicines = useAsync(() => pharmacyApi.medicines(), []);
  const [prescribing, setPrescribing] = useState(false);
  const [detail, setDetail] = useState(null);
  const [medicine, setMedicine] = useState(null);
  const [stock, setStock] = useState(null);
  const [ledger, setLedger] = useState(null);
  const { busy, run } = useAction();
  const { setData: setQueue } = queue;

  useEffect(() => {
    const timer = setInterval(() => pharmacyApi.queue().then(setQueue).catch(() => {}), 20000);
    return () => clearInterval(timer);
  }, [setQueue]);

  const reloadAll = () => {
    queue.reload();
    prescriptions.reload();
    medicines.reload();
  };

  const openDetail = async (row) => {
    const full = row.items?.length ? row : await run(() => pharmacyApi.prescription(row.id));
    if (full) setDetail(full);
  };

  const act = async (action, message) => {
    const saved = await run(() => action(detail.id), message);
    if (saved) {
      setDetail(saved);
      reloadAll();
    }
  };

  const saveMedicine = async () => {
    const body = {
      medicineName: medicine.medicineName.trim(),
      category: medicine.category.trim(),
      unitPrice: Number(medicine.unitPrice),
      supplyCategory: medicine.supplyCategory,
      pbsItemCode: medicine.supplyCategory === 'PBS' && medicine.pbsItemCode ? medicine.pbsItemCode.trim().toUpperCase() : null,
      stockQuantity: medicine.id ? null : Number(medicine.stockQuantity || 0),
      active: medicine.active,
    };
    const saved = await run(() => (medicine.id ? pharmacyApi.updateMedicine(medicine.id, body) : pharmacyApi.createMedicine(body)), 'Formulary updated');
    if (saved) {
      setMedicine(null);
      medicines.reload();
    }
  };

  const saveStock = async () => {
    if (await run(() => pharmacyApi.adjustStock(stock.id, Number(stock.delta), stock.reason), 'Stock updated')) {
      setStock(null);
      medicines.reload();
    }
  };

  const openLedger = async (m) => {
    const rows = await run(() => pharmacyApi.movements(m.id));
    if (rows) setLedger({ name: m.medicineName, rows });
  };

  const queueRows = queue.data || [];

  return (
    <>
      <PageHeader
        title="Pharmacy"
        subtitle="Only dispensed medicines are billed. PBS and prescription medicines are GST-free; OTC and other goods carry 10% GST."
        actions={
          <>
            {hasRole(can.manageMedicines) && (
              <Button variant="secondary" icon={PackagePlus} onClick={() => setMedicine({ medicineName: '', category: '', unitPrice: '', supplyCategory: 'PBS', pbsItemCode: '', stockQuantity: 100, active: true })}>
                Add medicine
              </Button>
            )}
            {hasRole(can.prescribe) && <Button icon={Pill} onClick={() => setPrescribing(true)}>New prescription</Button>}
          </>
        }
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
            <p className="text-sm text-slate-500">Prescriptions from doctors awaiting dispensing, oldest first. Dispensing deducts stock and adds the lines to the patient&apos;s bill.</p>
            <Button variant="secondary" icon={RefreshCw} onClick={queue.reload}>Refresh</Button>
          </div>
          <AsyncContent state={queue} isEmpty={queueRows.length === 0} emptyTitle="Nothing to dispense" emptyText="New prescriptions appear here automatically.">
            <ul className="divide-y divide-slate-100">
              {queueRows.map((p) => (
                <li key={p.id}>
                  <button type="button" onClick={() => openDetail(p)} className="w-full px-5 py-4 text-left hover:bg-blue-50/40">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <span className="flex items-center gap-2">
                        <b>{p.patientName}</b>
                        <span className="text-xs text-slate-500">{p.patientMrn} - {p.admissionNo}</span>
                        {!p.admissionActive && <StatusBadge status="DISCHARGED" />}
                        {!p.stockSufficient && <span className="inline-flex items-center gap-1 text-xs font-semibold text-rose-600"><AlertTriangle className="h-3 w-3" />insufficient stock</span>}
                      </span>
                      <span className="font-mono text-xs text-slate-500">{p.prescriptionNo} - {dateTime(p.prescriptionDate)}</span>
                    </div>
                    <p className="mt-1 text-sm text-slate-700">{p.items.map((i) => `${i.medicineName} x ${i.quantity} (${i.dose}, ${i.frequency})`).join('; ')}</p>
                    <div className="mt-2"><AuditChips chips={rxChips(p)} /></div>
                  </button>
                </li>
              ))}
            </ul>
          </AsyncContent>
        </div>
      )}

      {tab === 'prescriptions' && (
        <div className="card">
          <div className="flex flex-wrap gap-2 border-b border-slate-100 p-4">
            {['', 'PRESCRIBED', 'DISPENSED', 'CANCELLED'].map((s) => (
              <button key={s || 'ALL'} type="button" onClick={() => { setStatus(s); setPage(0); }} className={`rounded-full px-3 py-1 text-sm ${status === s ? 'bg-slate-800 text-white' : 'bg-slate-100 text-slate-600'}`}>
                {s ? label(s) : 'All'}
              </button>
            ))}
          </div>
          <AsyncContent state={prescriptions} isEmpty={prescriptions.data?.content.length === 0} emptyTitle="No prescriptions">
            {prescriptions.data && (
              <>
                <Table
                  rows={prescriptions.data.content}
                  onRowClick={openDetail}
                  columns={[
                    { key: 'prescriptionNo', header: 'Prescription', render: (p) => <span className="font-mono text-xs">{p.prescriptionNo}</span> },
                    { key: 'patientName', header: 'Patient', render: (p) => <b>{p.patientName}</b> },
                    { key: 'doctorName', header: 'Prescribed by' },
                    { key: 'dispensed', header: 'Dispensed by', render: (p) => p.audit?.dispensedByName || '-' },
                    { key: 'prescriptionDate', header: 'Date', render: (p) => dateTime(p.prescriptionDate) },
                    { key: 'status', header: 'Status', render: (p) => <StatusBadge status={p.status} /> },
                  ]}
                />
                <Pagination page={prescriptions.data} onChange={setPage} />
              </>
            )}
          </AsyncContent>
        </div>
      )}

      {tab === 'medicines' && (
        <div className="card">
          <AsyncContent state={medicines} isEmpty={(medicines.data || []).length === 0} emptyTitle="Formulary is empty" emptyText="Use Add medicine to create the first item.">
            {medicines.data && (
              <Table
                rows={medicines.data}
                columns={[
                  { key: 'medicineName', header: 'Medicine', render: (m) => <b className={m.active ? '' : 'text-slate-400 line-through'}>{m.medicineName}</b> },
                  { key: 'category', header: 'Category' },
                  { key: 'supplyCategory', header: 'Supply type', render: (m) => <>{supplyLabel(m.supplyCategory)}{m.pbsItemCode && <span className="block text-xs text-slate-400">PBS {m.pbsItemCode}</span>}</> },
                  { key: 'gst', header: 'GST', render: (m) => (m.gstFree ? <span className="text-emerald-700">GST-free</span> : '10%') },
                  { key: 'unitPrice', header: 'Unit price', render: (m) => money(m.unitPrice), className: 'text-right' },
                  {
                    key: 'stockQuantity',
                    header: 'Stock',
                    render: (m) => (
                      <span className={m.stockQuantity < 100 ? 'font-semibold text-rose-600' : ''}>
                        {m.stockQuantity}
                        {m.stockAdjustedByName && <span className="block text-xs font-normal text-slate-400">last: {m.stockAdjustedByName}, {dateTime(m.stockAdjustedAt)}</span>}
                      </span>
                    ),
                  },
                  {
                    key: 'actions',
                    header: '',
                    render: (m) => (
                      <span className="flex gap-1">
                        <Button variant="ghost" icon={History} onClick={() => openLedger(m)}>Ledger</Button>
                        {hasRole(can.manageMedicines) && (
                          <>
                            <Button variant="ghost" onClick={() => setStock({ id: m.id, name: m.medicineName, delta: 100, reason: '' })}>Adjust</Button>
                            <Button variant="ghost" icon={Pencil} onClick={() => setMedicine({ ...m, pbsItemCode: m.pbsItemCode || '' })} aria-label="Edit" />
                          </>
                        )}
                      </span>
                    ),
                  },
                ]}
              />
            )}
          </AsyncContent>
        </div>
      )}

      {prescribing && <PrescriptionForm open onClose={() => setPrescribing(false)} onSaved={() => { setPrescribing(false); reloadAll(); }} />}

      {detail && (
        <Modal
          open
          size="max-w-2xl"
          onClose={() => setDetail(null)}
          title={`${detail.prescriptionNo} - ${detail.patientName}`}
          footer={
            detail.status === 'PRESCRIBED' && detail.admissionActive && (
              <>
                {hasRole(['ADMIN', 'DOCTOR', 'PHARMACY']) && <Button variant="danger" loading={busy} onClick={() => act(pharmacyApi.cancel, 'Prescription cancelled')}>Cancel</Button>}
                {hasRole(can.dispense) && (
                  <Button variant="success" loading={busy} disabled={!detail.stockSufficient} onClick={() => act(pharmacyApi.dispense, 'Dispensed - stock updated and added to the bill')}>
                    Dispense
                  </Button>
                )}
              </>
            )
          }
        >
          <div className="mb-3 space-y-2 text-sm">
            <div className="flex items-center gap-2"><StatusBadge status={detail.status} /><span className="text-slate-500">{detail.admissionNo}</span></div>
            <AuditChips chips={rxChips(detail)} />
            {detail.status === 'PRESCRIBED' && !detail.admissionActive && (
              <p className="rounded-lg bg-amber-50 px-3 py-2 text-amber-800">The patient has been discharged and the bill is locked, so this prescription can no longer be dispensed or billed.</p>
            )}
          </div>
          <Table
            rows={detail.items}
            columns={[
              { key: 'medicineName', header: 'Medicine', render: (i) => <b>{i.medicineName}</b> },
              { key: 'quantity', header: 'Qty' },
              { key: 'dose', header: 'Dose' },
              { key: 'frequency', header: 'Frequency' },
              { key: 'unitPrice', header: 'Unit', render: (i) => money(i.unitPrice) },
              { key: 'gst', header: 'GST', render: (i) => (Number(i.gstRate) > 0 ? '10%' : 'Free') },
              { key: 'stock', header: 'In stock', render: (i) => <span className={i.stockAvailable < i.quantity ? 'font-semibold text-rose-600' : ''}>{i.stockAvailable}</span> },
            ]}
          />
        </Modal>
      )}

      {medicine && (
        <Modal open onClose={() => setMedicine(null)} title={medicine.id ? `Edit ${medicine.medicineName}` : 'Add medicine'} footer={<><Button variant="secondary" onClick={() => setMedicine(null)}>Cancel</Button><Button loading={busy} disabled={!medicine.medicineName.trim() || !medicine.category.trim() || medicine.unitPrice === ''} onClick={saveMedicine}>Save</Button></>}>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Name" required className="sm:col-span-2"><input className="input" disabled={!!medicine.id} maxLength={120} value={medicine.medicineName} onChange={(e) => setMedicine({ ...medicine, medicineName: e.target.value })} /></Field>
            <Field label="Category" required hint="e.g. Analgesic, Antibiotic"><input className="input" maxLength={60} value={medicine.category} onChange={(e) => setMedicine({ ...medicine, category: e.target.value })} /></Field>
            <Field label="Unit price (AUD, ex GST)" required><input type="number" min="0" step="0.01" className="input" value={medicine.unitPrice} onChange={(e) => setMedicine({ ...medicine, unitPrice: e.target.value })} /></Field>
            <Field label="Supply type (sets GST)" required className="sm:col-span-2">
              <select className="input" value={medicine.supplyCategory} onChange={(e) => setMedicine({ ...medicine, supplyCategory: e.target.value })}>
                {SUPPLY_CATEGORIES.map((c) => <option key={c.value} value={c.value}>{c.label} - {c.gst}</option>)}
              </select>
            </Field>
            {medicine.supplyCategory === 'PBS' && (
              <Field label="PBS item code (optional)"><input className="input uppercase" maxLength={10} value={medicine.pbsItemCode} onChange={(e) => setMedicine({ ...medicine, pbsItemCode: e.target.value })} /></Field>
            )}
            {!medicine.id && (
              <Field label="Opening stock"><input type="number" min="0" className="input" value={medicine.stockQuantity} onChange={(e) => setMedicine({ ...medicine, stockQuantity: e.target.value })} /></Field>
            )}
            <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={medicine.active} onChange={(e) => setMedicine({ ...medicine, active: e.target.checked })} /> Active (can be prescribed)</label>
          </div>
        </Modal>
      )}
      {stock && (
        <Modal open onClose={() => setStock(null)} title={`Adjust stock - ${stock.name}`} footer={<><Button variant="secondary" onClick={() => setStock(null)}>Cancel</Button><Button loading={busy} disabled={!Number(stock.delta)} onClick={saveStock}>Apply</Button></>}>
          <div className="grid gap-4">
            <Field label="Change" hint="Positive = stock received, negative = write-off"><input type="number" className="input" value={stock.delta} onChange={(e) => setStock({ ...stock, delta: e.target.value })} /></Field>
            <Field label="Reason"><input className="input" maxLength={250} placeholder="e.g. Delivery INV-4471, expired batch" value={stock.reason} onChange={(e) => setStock({ ...stock, reason: e.target.value })} /></Field>
          </div>
        </Modal>
      )}
      {ledger && (
        <Modal open size="max-w-2xl" onClose={() => setLedger(null)} title={`Stock ledger - ${ledger.name}`} footer={<Button onClick={() => setLedger(null)}>Close</Button>}>
          <Table
            rows={ledger.rows}
            columns={[
              { key: 'performedAt', header: 'When', render: (r) => dateTime(r.performedAt) },
              { key: 'movementType', header: 'Type', render: (r) => label(r.movementType) },
              { key: 'quantityDelta', header: 'Change', render: (r) => <span className={r.quantityDelta < 0 ? 'text-rose-600' : 'text-emerald-700'}>{r.quantityDelta > 0 ? `+${r.quantityDelta}` : r.quantityDelta}</span>, className: 'text-right' },
              { key: 'balanceAfter', header: 'Balance', className: 'text-right' },
              { key: 'performedByName', header: 'By' },
              { key: 'reason', header: 'Reason', render: (r) => <>{r.reason}{r.prescriptionNo && <span className="block font-mono text-xs text-slate-400">{r.prescriptionNo}</span>}</> },
            ]}
          />
        </Modal>
      )}
    </>
  );
}
