import { useEffect, useState } from 'react';
import { BedDouble, Plus, RefreshCw, Trash2 } from 'lucide-react';
import { roomApi } from '../api';
import useAsync from '../hooks/useAsync';
import useAction from '../hooks/useAction';
import { useAuth } from '../auth/AuthContext';
import { can } from '../auth/roles';
import PageHeader from '../components/ui/PageHeader';
import Button from '../components/ui/Button';
import Modal from '../components/ui/Modal';
import Field from '../components/ui/Field';
import StatusBadge from '../components/ui/StatusBadge';
import { AsyncContent } from '../components/ui/States';
import { label, money } from '../utils/format';

const TYPES = ['GENERAL_WARD', 'SEMI_PRIVATE', 'PRIVATE', 'ICU'];
const BORDER = { AVAILABLE: 'border-emerald-200', OCCUPIED: 'border-blue-200', UNDER_MAINTENANCE: 'border-amber-200' };
const FILTERS = [['', 'All'], ['AVAILABLE', 'Available'], ['OCCUPIED', 'Occupied'], ['UNDER_MAINTENANCE', 'Under maintenance']];

export default function RoomsPage() {
  const { hasRole } = useAuth();
  const state = useAsync(() => roomApi.list(), []);
  const [editing, setEditing] = useState(null);
  const [filter, setFilter] = useState('');
  const { busy, run } = useAction();
  const { setData } = state;

  // Live bed board: silently re-poll so occupancy changes made at other desks show up.
  useEffect(() => {
    const timer = setInterval(() => roomApi.list().then(setData).catch(() => {}), 15000);
    return () => clearInterval(timer);
  }, [setData]);

  const remove = async () => {
    const ok = await run(() => roomApi.remove(editing.id), 'Bed deleted');
    if (ok !== undefined) {
      setEditing(null);
      state.reload();
    }
  };

  const save = async () => {
    const body = { ...editing, dailyRate: Number(editing.dailyRate) };
    const saved = await run(() => (editing.id ? roomApi.update(editing.id, body) : roomApi.create(body)), 'Room saved');
    if (saved) {
      setEditing(null);
      state.reload();
    }
  };

  const allRooms = state.data || [];
  const rooms = filter ? allRooms.filter((r) => r.status === filter) : allRooms;
  const counts = allRooms.reduce((acc, r) => ({ ...acc, [r.status]: (acc[r.status] || 0) + 1 }), {});

  return (
    <>
      <PageHeader
        title="Rooms & beds"
        subtitle={`${counts.AVAILABLE || 0} available - ${counts.OCCUPIED || 0} occupied - ${counts.UNDER_MAINTENANCE || 0} under maintenance`}
        actions={(
          <>
            <Button variant="secondary" icon={RefreshCw} onClick={state.reload}>Refresh</Button>
            {hasRole(can.manageRooms) && (
              <Button icon={Plus} onClick={() => setEditing({ roomNo: '', bedNo: 'A', roomType: 'GENERAL_WARD', dailyRate: 450, status: 'AVAILABLE' })}>Add bed</Button>
            )}
          </>
        )}
      />
      <div className="mb-4 flex flex-wrap gap-2">
        {FILTERS.map(([value, text]) => (
          <button
            key={value || 'all'}
            type="button"
            onClick={() => setFilter(value)}
            className={`rounded-full px-3 py-1 text-sm font-medium ring-1 ring-inset ${filter === value ? 'bg-blue-600 text-white ring-blue-600' : 'bg-white text-slate-600 ring-slate-200 hover:bg-slate-50'}`}
          >
            {text}{value && ` (${counts[value] || 0})`}
          </button>
        ))}
      </div>
      <AsyncContent state={state} isEmpty={rooms.length === 0} emptyTitle="No rooms configured">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {rooms.map((r) => (
            <button
              key={r.id}
              type="button"
              disabled={!hasRole(can.manageRooms)}
              onClick={() => setEditing({ ...r })}
              className={`card border-2 p-4 text-left ${BORDER[r.status]} disabled:cursor-default`}
            >
              <div className="flex items-center justify-between">
                <BedDouble className="h-5 w-5 text-slate-400" />
                <StatusBadge status={r.status} />
              </div>
              <p className="mt-3 text-lg font-bold">Room {r.roomNo} / {r.bedNo}</p>
              <p className="text-sm text-slate-500">{label(r.roomType)}</p>
              <p className="mt-2 text-sm font-semibold">{money(r.dailyRate)} / day</p>
            </button>
          ))}
        </div>
      </AsyncContent>
      {editing && (
        <Modal
          open
          onClose={() => setEditing(null)}
          title={editing.id ? `Room ${editing.roomNo} / ${editing.bedNo}` : 'New bed'}
          footer={(
            <>
              {editing.id && editing.status !== 'OCCUPIED' && (
                <Button variant="danger" icon={Trash2} loading={busy} onClick={remove} className="mr-auto">Delete bed</Button>
              )}
              <Button variant="secondary" onClick={() => setEditing(null)}>Cancel</Button>
              <Button loading={busy} onClick={save}>Save</Button>
            </>
          )}
        >
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Room no" required><input className="input" disabled={!!editing.id} value={editing.roomNo} onChange={(e) => setEditing({ ...editing, roomNo: e.target.value })} /></Field>
            <Field label="Bed no" required><input className="input" disabled={!!editing.id} value={editing.bedNo} onChange={(e) => setEditing({ ...editing, bedNo: e.target.value })} /></Field>
            <Field label="Type">
              <select className="input" value={editing.roomType} onChange={(e) => setEditing({ ...editing, roomType: e.target.value })}>
                {TYPES.map((t) => <option key={t} value={t}>{label(t)}</option>)}
              </select>
            </Field>
            <Field label="Daily rate (AUD)"><input type="number" min="0" className="input" value={editing.dailyRate} onChange={(e) => setEditing({ ...editing, dailyRate: e.target.value })} /></Field>
            {editing.status !== 'OCCUPIED' && (
              <Field label="Status">
                <select className="input" value={editing.status} onChange={(e) => setEditing({ ...editing, status: e.target.value })}>
                  <option value="AVAILABLE">Available</option>
                  <option value="UNDER_MAINTENANCE">Under maintenance</option>
                </select>
              </Field>
            )}
          </div>
        </Modal>
      )}
    </>
  );
}
