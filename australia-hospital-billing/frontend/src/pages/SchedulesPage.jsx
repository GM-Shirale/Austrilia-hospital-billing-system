import { useMemo, useState } from 'react';
import { CalendarClock, Pencil, Plus, Trash2 } from 'lucide-react';
import { providerApi } from '../api';
import useAsync from '../hooks/useAsync';
import useAction from '../hooks/useAction';
import { useAuth } from '../auth/AuthContext';
import { can } from '../auth/roles';
import PageHeader from '../components/ui/PageHeader';
import Button from '../components/ui/Button';
import Modal from '../components/ui/Modal';
import Field from '../components/ui/Field';
import { AsyncContent } from '../components/ui/States';
import { label } from '../utils/format';

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
const hhmm = (t) => (t ? t.slice(0, 5) : '');
const todayName = DAYS[(new Date().getDay() + 6) % 7];

/** Weekly roster: rows = doctors, columns = days. Receptionists use it to see who is on today. */
export default function SchedulesPage() {
  const { hasRole } = useAuth();
  const doctors = useAsync(() => providerApi.doctors(undefined, 0, 100), []);
  const schedules = useAsync(() => providerApi.allSchedules(), []);
  const [editing, setEditing] = useState(null);
  const { busy, run } = useAction();
  const canEdit = hasRole(can.manageSchedules);

  const byDoctor = useMemo(() => {
    const map = {};
    (schedules.data || []).forEach((s) => {
      map[s.doctorId] ??= {};
      (map[s.doctorId][s.dayOfWeek] ??= []).push(s);
    });
    return map;
  }, [schedules.data]);

  const doctorList = (doctors.data?.content || []).filter((d) => d.active);
  const onToday = doctorList.filter((d) => byDoctor[d.id]?.[todayName]?.length);

  const save = async () => {
    const body = {
      dayOfWeek: editing.dayOfWeek,
      startTime: editing.startTime,
      endTime: editing.endTime,
      slotMinutes: Number(editing.slotMinutes),
      location: editing.location || null,
    };
    const saved = await run(
      () => (editing.id ? providerApi.updateSchedule(editing.id, body) : providerApi.addSchedule(editing.doctorId, body)),
      'Schedule saved',
    );
    if (saved) {
      setEditing(null);
      schedules.reload();
    }
  };

  const remove = async () => {
    const ok = await run(() => providerApi.deleteSchedule(editing.id), 'Session removed');
    if (ok !== undefined) {
      setEditing(null);
      schedules.reload();
    }
  };

  const newSession = (doctorId, dayOfWeek = 'MONDAY') =>
    setEditing({ doctorId, dayOfWeek, startTime: '09:00', endTime: '13:00', slotMinutes: 15, location: '' });

  return (
    <>
      <PageHeader
        title="Doctor schedules"
        subtitle={`${onToday.length} of ${doctorList.length} doctors rostered today (${label(todayName)})`}
      />
      <AsyncContent state={schedules.data ? doctors : schedules} isEmpty={doctorList.length === 0} emptyTitle="No doctors yet">
        <div className="card overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
              <tr>
                <th className="px-4 py-3">Doctor</th>
                {DAYS.map((d) => (
                  <th key={d} className={`px-3 py-3 ${d === todayName ? 'text-blue-700' : ''}`}>{label(d).slice(0, 3)}</th>
                ))}
                {canEdit && <th className="px-3 py-3" />}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {doctorList.map((doc) => (
                <tr key={doc.id} className="align-top">
                  <td className="px-4 py-3">
                    <p className="font-semibold text-slate-800">{doc.displayName}</p>
                    <p className="text-xs text-slate-500">{doc.departmentName} - {doc.providerNo}</p>
                  </td>
                  {DAYS.map((day) => (
                    <td key={day} className={`px-2 py-2 ${day === todayName ? 'bg-blue-50/40' : ''}`}>
                      <div className="space-y-1">
                        {(byDoctor[doc.id]?.[day] || []).map((s) => (
                          <button
                            key={s.id}
                            type="button"
                            disabled={!canEdit}
                            title={s.location || ''}
                            onClick={() => setEditing({ ...s, startTime: hhmm(s.startTime), endTime: hhmm(s.endTime), location: s.location || '' })}
                            className="block w-full whitespace-nowrap rounded-md bg-emerald-50 px-2 py-1 text-left text-xs font-medium text-emerald-800 ring-1 ring-inset ring-emerald-200 enabled:hover:bg-emerald-100 disabled:cursor-default"
                          >
                            {hhmm(s.startTime)}-{hhmm(s.endTime)}
                          </button>
                        ))}
                      </div>
                    </td>
                  ))}
                  {canEdit && (
                    <td className="px-3 py-2">
                      <Button variant="ghost" icon={Plus} onClick={() => newSession(doc.id)} aria-label={`Add session for ${doc.displayName}`}>Add</Button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <p className="mt-3 flex items-center gap-2 text-xs text-slate-500">
          <CalendarClock className="h-4 w-4" /> Consultations recorded outside a doctor&apos;s sessions are flagged &quot;off roster&quot; on the admission.
        </p>
      </AsyncContent>

      {editing && (
        <Modal
          open
          onClose={() => setEditing(null)}
          title={editing.id ? `Edit session - ${editing.doctorName}` : 'New clinic session'}
          footer={(
            <>
              {editing.id && <Button variant="danger" icon={Trash2} loading={busy} onClick={remove} className="mr-auto">Remove</Button>}
              <Button variant="secondary" onClick={() => setEditing(null)}>Cancel</Button>
              <Button icon={Pencil} loading={busy} onClick={save}>Save</Button>
            </>
          )}
        >
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Day" required>
              <select className="input" value={editing.dayOfWeek} onChange={(e) => setEditing({ ...editing, dayOfWeek: e.target.value })}>
                {DAYS.map((d) => <option key={d} value={d}>{label(d)}</option>)}
              </select>
            </Field>
            <Field label="Slot length (minutes)" required>
              <input type="number" min="5" max="120" className="input" value={editing.slotMinutes} onChange={(e) => setEditing({ ...editing, slotMinutes: e.target.value })} />
            </Field>
            <Field label="Start" required>
              <input type="time" className="input" value={editing.startTime} onChange={(e) => setEditing({ ...editing, startTime: e.target.value })} />
            </Field>
            <Field label="End" required>
              <input type="time" className="input" value={editing.endTime} onChange={(e) => setEditing({ ...editing, endTime: e.target.value })} />
            </Field>
            <Field label="Location" className="sm:col-span-2">
              <input className="input" value={editing.location} maxLength={100} placeholder="e.g. Cardiology Clinic, Level 2" onChange={(e) => setEditing({ ...editing, location: e.target.value })} />
            </Field>
          </div>
        </Modal>
      )}
    </>
  );
}
