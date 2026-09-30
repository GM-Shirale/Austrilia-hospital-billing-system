import { useNavigate } from 'react-router-dom';
import { Activity, BedDouble, DollarSign, FlaskConical, Pill, Receipt, Users, Wallet } from 'lucide-react';
import { dashboardApi } from '../api';
import useAsync from '../hooks/useAsync';
import { useAuth } from '../auth/AuthContext';
import PageHeader from '../components/ui/PageHeader';
import Stat from '../components/ui/Stat';
import StatusBadge from '../components/ui/StatusBadge';
import { AsyncContent } from '../components/ui/States';
import { money } from '../utils/format';

const FLOW = ['Register patient', 'Admit & allocate bed', 'Lab / pharmacy / doctor charges', 'Discharge',
  'Generate & finalise bill', 'Create & validate claims', 'Submit (async via events)', 'Remittance, EOB & reconcile'];

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const state = useAsync(() => dashboardApi.summary(), []);
  const s = state.data;

  return (
    <>
      <PageHeader title={`Good day, ${user.fullName.split(' ')[0]}`} subtitle={`${user.hospitalName} - live overview`} />
      <AsyncContent state={state}>
        {s && (
          <>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <Stat icon={Users} label="Active patients" value={s.activePatients} onClick={() => navigate('/patients')} />
              <Stat icon={Activity} label="Current admissions" value={s.currentAdmissions} tone="violet" onClick={() => navigate('/admissions')} />
              <Stat icon={BedDouble} label="Beds free / occupied" value={`${s.availableBeds} / ${s.occupiedBeds}`} tone="green" onClick={() => navigate('/rooms')} />
              <Stat icon={FlaskConical} label="Pending lab orders" value={s.pendingLabOrders} tone="amber" onClick={() => navigate('/lab')} />
              <Stat icon={Pill} label="Low-stock medicines" value={s.lowStockMedicines} tone="rose" onClick={() => navigate('/pharmacy')} />
              <Stat icon={Receipt} label="Draft bills" value={s.draftBills} onClick={() => navigate('/bills')} />
              <Stat icon={Wallet} label="Outstanding" value={money(s.outstandingAmount)} tone="amber" onClick={() => navigate('/bills')} />
              <Stat icon={DollarSign} label="Collected" value={money(s.collectedAmount)} tone="green" onClick={() => navigate('/bills')} />
            </div>

            <div className="mt-6 grid gap-6 lg:grid-cols-3">
              <div className="card p-5 lg:col-span-2">
                <h2 className="font-bold text-slate-900">Billing workflow</h2>
                <ol className="mt-4 grid gap-3 sm:grid-cols-2">
                  {FLOW.map((step, i) => (
                    <li key={step} className="flex items-center gap-3 rounded-xl bg-slate-50 px-3 py-2 text-sm">
                      <span className="flex h-6 w-6 items-center justify-center rounded-full bg-blue-600 text-xs font-bold text-white">{i + 1}</span>
                      {step}
                    </li>
                  ))}
                </ol>
              </div>
              <div className="card p-5">
                <h2 className="font-bold text-slate-900">Claims by status</h2>
                {Object.keys(s.claimsByStatus).length === 0 ? (
                  <p className="mt-4 text-sm text-slate-500">No claims yet.</p>
                ) : (
                  <ul className="mt-4 space-y-2">
                    {Object.entries(s.claimsByStatus).map(([status, count]) => (
                      <li key={status} className="flex items-center justify-between">
                        <StatusBadge status={status} />
                        <span className="font-semibold">{count}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          </>
        )}
      </AsyncContent>
    </>
  );
}
