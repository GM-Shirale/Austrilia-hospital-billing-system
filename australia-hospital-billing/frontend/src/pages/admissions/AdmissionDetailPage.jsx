import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  ArrowLeft, BedDouble, FlaskConical, Lock, LogOut, Pill, Receipt, Stethoscope, UserCog, XCircle,
} from 'lucide-react';
import { admissionApi, billingApi, labApi, pharmacyApi, providerApi, roomApi } from '../../api';
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
import InvoiceButton from '../../components/InvoiceButton';
import { AsyncContent } from '../../components/ui/States';
import { dateTime, label, money, toLocalInput } from '../../utils/format';
import LabOrderForm from './LabOrderForm';
import PrescriptionForm from './PrescriptionForm';
import ConsultationForm from './ConsultationForm';

const DAY_MS = 86400000;
/** Same rule as the backend: every started 24 h is a day, minimum 1. */
const stayDays = (from, to) => Math.max(1, Math.ceil((new Date(to) - new Date(from)) / DAY_MS));

export default function AdmissionDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const state = useAsync(() => admissionApi.get(id), [id]);
  const consultations = useAsync(() => admissionApi.consultations(id), [id]);
  const labOrders = useAsync(() => labApi.forAdmission(id), [id]);
  const prescriptions = useAsync(() => pharmacyApi.forAdmission(id), [id]);
  const [modal, setModal] = useState(null);
  const [rooms, setRooms] = useState([]);
  const [roomId, setRoomId] = useState('');
  const [doctors, setDoctors] = useState([]);
  const [doctorId, setDoctorId] = useState('');
  const [dischargeAt, setDischargeAt] = useState('');
  const [bill, setBill] = useState(null);
  const { busy, run } = useAction();
  const data = state.data;
  const a = data?.admission;
  const active = a?.status === 'ADMITTED';
  const inpatient = a?.careSetting !== 'OPD';

  const loadBill = useCallback(() => {
    if (!hasRole(can.viewInvoice)) return;
    admissionApi.bills(id)
      .then((page) => setBill(page.content.find((b) => b.status !== 'CANCELLED') || null))
      .catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  useEffect(loadBill, [loadBill]);

  const openBedModal = async () => {
    const list = await roomApi.list('AVAILABLE');
    setRooms(list);
    setRoomId(list[0]?.id ?? '');
    setModal('bed');
  };

  const openDoctorModal = async () => {
    const page = await providerApi.doctors(undefined, 0, 100);
    setDoctors(page.content.filter((d) => d.active));
    setDoctorId(a.doctorId);
    setModal('doctor');
  };

  const allocate = async () => {
    if (await run(() => admissionApi.allocateBed(id, Number(roomId)), 'Bed allocated - now OCCUPIED')) {
      setModal(null);
      state.reload();
    }
  };

  const changeDoctor = async () => {
    const saved = await run(() => admissionApi.changeDoctor(id, Number(doctorId)), 'Attending doctor updated');
    if (saved) {
      state.setData(saved);
      setModal(null);
    }
  };

  const discharge = async () => {
    const saved = await run(
      () => admissionApi.discharge(id, dischargeAt || null),
      'Patient discharged - bed released, bill calculated and locked',
    );
    if (saved) {
      state.setData(saved);
      setModal(null);
      loadBill();
      consultations.reload();
    }
  };

  const cancel = async () => {
    const saved = await run(() => admissionApi.cancel(id), 'Admission cancelled');
    if (saved) state.setData(saved);
  };

  const cancelConsultation = async (c) => {
    if (await run(() => admissionApi.cancelConsultation(c.id), `Consultation ${c.consultationNo} cancelled`)) consultations.reload();
  };

  const generateBill = async () => {
    const created = await run(() => billingApi.generate(Number(id)), 'Draft bill generated');
    if (created) navigate(`/bills/${created.id}`);
  };

  const orderContext = a && { id: a.id, admissionNo: a.admissionNo, doctorId: a.doctorId, doctorName: a.doctorName };
  const previewEnd = dischargeAt || toLocalInput();
  const previewDays = a ? stayDays(a.admissionDate, previewEnd) : 0;

  return (
    <>
      <Link to="/admissions" className="mb-3 inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-800">
        <ArrowLeft className="h-4 w-4" /> Admissions
      </Link>
      <AsyncContent state={state}>
        {a && (
          <>
            <PageHeader
              title={`${a.patientName} - ${a.admissionNo}`}
              subtitle={`${a.admissionType} - ${label(a.financialClass)} ${inpatient ? 'in-patient (IPD)' : 'out-patient (OPD)'} - ${data.departmentName}`}
              actions={
                active && (
                  <>
                    {inpatient && hasRole(can.allocateBed) && <Button variant="secondary" icon={BedDouble} onClick={openBedModal}>{data.currentBed ? 'Transfer bed' : 'Allocate bed'}</Button>}
                    {hasRole(can.changeDoctor) && <Button variant="secondary" icon={UserCog} onClick={openDoctorModal}>Attending doctor</Button>}
                    {hasRole(can.consult) && <Button variant="secondary" icon={Stethoscope} onClick={() => setModal('consult')}>Consultation</Button>}
                    {hasRole(can.orderLab) && <Button variant="secondary" icon={FlaskConical} onClick={() => setModal('lab')}>Order lab</Button>}
                    {hasRole(can.prescribe) && <Button variant="secondary" icon={Pill} onClick={() => setModal('rx')}>Prescribe</Button>}
                    {hasRole(can.admit) && <Button icon={LogOut} onClick={() => { setDischargeAt(''); setModal('discharge'); }}>Discharge</Button>}
                    {hasRole(can.cancelAdmission) && <Button variant="ghost" icon={XCircle} onClick={cancel}>Cancel</Button>}
                  </>
                )
              }
            />
            <div className="grid gap-6 lg:grid-cols-3">
              <div className="card grid grid-cols-2 gap-4 p-5 lg:col-span-2 sm:grid-cols-3">
                <div><p className="label">Status</p><StatusBadge status={a.status} /></div>
                <div><p className="label">Care setting</p><StatusBadge status={a.careSetting} /></div>
                <div><p className="label">Length of stay</p><p className="text-sm font-semibold">{a.lengthOfStayDays} day(s){active ? ' so far' : ''}</p></div>
                <div><p className="label">Patient</p><Link className="text-sm text-blue-600" to={`/patients/${a.patientId}`}>{a.patientName} ({a.patientMrn})</Link></div>
                <div><p className="label">Attending doctor</p><p className="text-sm">{a.doctorName}</p><p className="text-xs text-slate-500">Provider {data.doctorProviderNo}</p></div>
                <div><p className="label">Medicare</p><p className="text-sm">{data.patientMedicareNo || 'No Medicare card'}</p></div>
                <div><p className="label">Admitted</p><p className="text-sm">{dateTime(a.admissionDate)}</p></div>
                <div><p className="label">Discharged</p><p className="text-sm">{dateTime(a.dischargeDate)}</p></div>
                <div><p className="label">Current bed</p><p className="text-sm">{data.currentBed ? `Room ${data.currentBed.roomNo} / ${data.currentBed.bedNo}` : (inpatient ? 'None' : 'n/a (OPD)')}</p></div>
                <div className="col-span-2 sm:col-span-3"><p className="label">Diagnosis</p><p className="text-sm">{data.diagnosis || '-'}</p></div>
              </div>

              <div className="card p-5">
                <h2 className="flex items-center gap-2 font-bold">Billing {bill?.status && bill.status !== 'DRAFT' ? <Lock className="h-4 w-4 text-slate-400" /> : null}</h2>
                {!hasRole(can.viewInvoice) && <p className="mt-2 text-sm text-slate-500">Billing staff issue the tax invoice after discharge.</p>}
                {bill && (
                  <div className="mt-3 space-y-3 text-sm">
                    <p className="flex flex-wrap items-center gap-2">
                      <span className="font-mono">{bill.billNo}</span> <StatusBadge status={bill.status} /> <StatusBadge status={bill.paymentStatus} />
                    </p>
                    <dl className="grid grid-cols-2 gap-1">
                      <dt className="text-slate-500">Total charges</dt><dd className="text-right font-semibold">{money(bill.netAmount)}</dd>
                      <dt className="text-slate-500">Patient pays</dt><dd className="text-right font-semibold">{money(bill.patientPayableAmount)}</dd>
                      <dt className="text-slate-500">Balance</dt><dd className="text-right font-semibold text-rose-600">{money(bill.patientBalance)}</dd>
                    </dl>
                    {!active && <p className="flex items-center gap-1 text-xs text-slate-500"><Lock className="h-3 w-3" /> Charges were calculated and locked at discharge.</p>}
                    <div className="flex flex-wrap gap-2">
                      {hasRole(can.billing) && <Button variant="secondary" icon={Receipt} onClick={() => navigate(`/bills/${bill.id}`)}>Open bill</Button>}
                      <InvoiceButton billId={bill.id} />
                    </div>
                  </div>
                )}
                {hasRole(can.billing) && !bill && a.status !== 'CANCELLED' && (
                  <div className="mt-3 space-y-2">
                    <p className="text-sm text-slate-500">Preview an interim bill now; it is recalculated and locked automatically at discharge.</p>
                    <Button icon={Receipt} loading={busy} onClick={generateBill}>Generate interim bill</Button>
                  </div>
                )}
                {hasRole(can.viewInvoice) && !hasRole(can.billing) && !bill && (
                  <p className="mt-2 text-sm text-slate-500">The bill is created automatically when the patient is discharged.</p>
                )}
              </div>
            </div>

            <section className="card mt-6">
              <h2 className="flex items-center gap-2 border-b border-slate-100 px-5 py-4 font-bold"><Stethoscope className="h-4 w-4 text-slate-400" /> Consultations</h2>
              {(consultations.data || []).length === 0 ? (
                <p className="p-5 text-sm text-slate-500">No consultations recorded. The attending doctor&apos;s admission attendance is billed automatically.</p>
              ) : (
                <Table
                  rows={consultations.data}
                  columns={[
                    { key: 'consultationNo', header: 'No', render: (c) => <span className="font-mono text-xs">{c.consultationNo}</span> },
                    { key: 'consultationAt', header: 'When', render: (c) => dateTime(c.consultationAt) },
                    { key: 'doctorName', header: 'Doctor', render: (c) => <span>{c.doctorName}<span className="block text-xs text-slate-500">{c.specialization}</span></span> },
                    { key: 'consultationType', header: 'Type', render: (c) => label(c.consultationType) },
                    { key: 'mbsItemNo', header: 'MBS', render: (c) => `${c.mbsItemNo} (${money(c.mbsScheduleFee)})` },
                    { key: 'fee', header: 'Fee', render: (c) => money(c.fee) },
                    { key: 'roster', header: 'Roster', render: (c) => (c.withinSchedule ? <span className="text-xs text-emerald-600">In session</span> : <span className="text-xs text-amber-600">Off roster</span>) },
                    { key: 'status', header: 'Status', render: (c) => <StatusBadge status={c.status} /> },
                    {
                      key: 'actions',
                      header: '',
                      render: (c) => active && c.status === 'COMPLETED' && hasRole(can.cancelConsult) && (
                        <Button variant="ghost" onClick={() => cancelConsultation(c)}>Cancel</Button>
                      ),
                    },
                  ]}
                />
              )}
            </section>

            <div className="mt-6 grid gap-6 lg:grid-cols-2">
              <section className="card">
                <h2 className="flex items-center gap-2 border-b border-slate-100 px-5 py-4 font-bold"><FlaskConical className="h-4 w-4 text-slate-400" /> Lab orders (MBS)</h2>
                {(labOrders.data || []).length === 0 ? <p className="p-5 text-sm text-slate-500">No lab orders.</p> : (
                  <ul className="divide-y divide-slate-100">
                    {labOrders.data.map((o) => (
                      <li key={o.id} className="px-5 py-3 text-sm">
                        <div className="flex items-center justify-between"><span className="font-mono text-xs">{o.orderNo}</span><StatusBadge status={o.status} /></div>
                        <p className="mt-1 text-slate-600">{o.items.map((i) => `${i.testName}${i.mbsItemNo ? ` (MBS ${i.mbsItemNo})` : ''}`).join(', ')}</p>
                        <p className="mt-1 text-xs text-slate-400">
                          Ordered by {o.doctorName}
                          {o.audit?.performedByName && ` - performed by ${o.audit.performedByName}`}
                          {o.audit?.resultNotes && ` - ${o.audit.resultNotes}`}
                        </p>
                      </li>
                    ))}
                  </ul>
                )}
              </section>
              <section className="card">
                <h2 className="flex items-center gap-2 border-b border-slate-100 px-5 py-4 font-bold"><Pill className="h-4 w-4 text-slate-400" /> Prescriptions</h2>
                {(prescriptions.data || []).length === 0 ? <p className="p-5 text-sm text-slate-500">No prescriptions.</p> : (
                  <ul className="divide-y divide-slate-100">
                    {prescriptions.data.map((p) => (
                      <li key={p.id} className="px-5 py-3 text-sm">
                        <div className="flex items-center justify-between"><span className="font-mono text-xs">{p.prescriptionNo}</span><StatusBadge status={p.status} /></div>
                        <p className="mt-1 text-slate-600">{p.items.map((i) => `${i.medicineName} x ${i.quantity}`).join(', ')}</p>
                        <p className="mt-1 text-xs text-slate-400">
                          Prescribed by {p.doctorName}
                          {p.audit?.dispensedByName ? ` - dispensed by ${p.audit.dispensedByName}` : p.status === 'PRESCRIBED' ? ' - awaiting pharmacy (not billed yet)' : ''}
                        </p>
                      </li>
                    ))}
                  </ul>
                )}
                <p className="px-5 pb-3 text-xs text-slate-400">Only DISPENSED medicines are billed.</p>
              </section>
            </div>

            {inpatient && (
              <section className="card mt-6">
                <h2 className="flex items-center gap-2 border-b border-slate-100 px-5 py-4 font-bold"><BedDouble className="h-4 w-4 text-slate-400" /> Bed history</h2>
                {data.bedHistory.length === 0 ? (
                  <p className="p-5 text-sm text-slate-500">No bed allocated yet.</p>
                ) : (
                  <Table
                    rows={data.bedHistory}
                    columns={[
                      { key: 'room', header: 'Bed', render: (b) => <b>Room {b.roomNo} / {b.bedNo}</b> },
                      { key: 'roomType', header: 'Type', render: (b) => label(b.roomType) },
                      { key: 'fromDate', header: 'From', render: (b) => dateTime(b.fromDate) },
                      { key: 'toDate', header: 'To', render: (b) => (b.toDate ? dateTime(b.toDate) : <StatusBadge status="OCCUPIED" />) },
                      { key: 'chargePerDay', header: 'Rate / day', render: (b) => money(b.chargePerDay) },
                    ]}
                  />
                )}
              </section>
            )}
          </>
        )}
      </AsyncContent>

      <Modal
        open={modal === 'bed'}
        onClose={() => setModal(null)}
        title={data?.currentBed ? 'Transfer to another bed' : 'Allocate bed'}
        footer={<><Button variant="secondary" onClick={() => setModal(null)}>Cancel</Button><Button loading={busy} disabled={!roomId} onClick={allocate}>Allocate</Button></>}
      >
        <Field label="Available beds (live)">
          <select className="input" value={roomId} onChange={(e) => setRoomId(e.target.value)}>
            {rooms.length === 0 && <option value="">No beds available</option>}
            {rooms.map((r) => <option key={r.id} value={r.id}>Room {r.roomNo}/{r.bedNo} - {label(r.roomType)} - {money(r.dailyRate)}/day</option>)}
          </select>
        </Field>
      </Modal>

      <Modal
        open={modal === 'doctor'}
        onClose={() => setModal(null)}
        title="Attending doctor"
        footer={<><Button variant="secondary" onClick={() => setModal(null)}>Cancel</Button><Button loading={busy} disabled={!doctorId} onClick={changeDoctor}>Save</Button></>}
      >
        <Field label="Doctor responsible for this admission" hint="The attending doctor's admission attendance is billed with their MBS item.">
          <select className="input" value={doctorId} onChange={(e) => setDoctorId(e.target.value)}>
            {doctors.map((d) => <option key={d.id} value={d.id}>{d.displayName} - {d.specialization} ({d.providerNo})</option>)}
          </select>
        </Field>
      </Modal>

      <Modal
        open={modal === 'discharge'}
        onClose={() => setModal(null)}
        title="Discharge patient"
        footer={<><Button variant="secondary" onClick={() => setModal(null)}>Cancel</Button><Button loading={busy} onClick={discharge}>Discharge & lock bill</Button></>}
      >
        <div className="space-y-4">
          <Field label="Discharge date & time (optional)" hint="Leave empty to discharge now (hospital time, Australia/Sydney).">
            <input type="datetime-local" className="input" value={dischargeAt} min={a ? toLocalInput(new Date(a.admissionDate)) : undefined} max={toLocalInput(new Date(Date.now() + DAY_MS))} onChange={(e) => setDischargeAt(e.target.value)} />
          </Field>
          {a && (
            <div className="rounded-lg bg-slate-50 p-3 text-sm">
              <p className="font-semibold">On discharge the system will:</p>
              <ul className="mt-1 list-disc space-y-0.5 pl-5 text-slate-600">
                <li>calculate the stay: <b>{previewDays} day(s)</b>{data.currentBed && <> x {money(data.currentBed.chargePerDay)} = <b>{money(previewDays * Number(data.currentBed.chargePerDay))}</b> accommodation</>}</li>
                {data.currentBed && <li>release Room {data.currentBed.roomNo}/{data.currentBed.bedNo} back to <b>AVAILABLE</b></li>}
                <li>generate or refresh the bill with Medicare / fund rebates</li>
                <li><b>lock</b> all clinical line items (no more lab, pharmacy or consultation changes)</li>
              </ul>
            </div>
          )}
        </div>
      </Modal>

      {modal === 'consult' && a && (
        <ConsultationForm
          admission={a}
          defaultDoctorId={a.doctorId}
          onClose={() => setModal(null)}
          onSaved={() => { setModal(null); consultations.reload(); }}
        />
      )}
      {modal === 'lab' && <LabOrderForm open admission={orderContext} onClose={() => setModal(null)} onSaved={() => { setModal(null); labOrders.reload(); }} />}
      {modal === 'rx' && <PrescriptionForm open admission={orderContext} onClose={() => setModal(null)} onSaved={() => { setModal(null); prescriptions.reload(); }} />}
    </>
  );
}
