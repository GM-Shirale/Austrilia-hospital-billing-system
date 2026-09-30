import { useEffect, useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import {
  Activity, BedDouble, FileCheck2, FlaskConical, LayoutDashboard, LogOut, Pill, Receipt,
  ShieldCheck, Sparkles, Stethoscope, Users, CalendarClock } from 'lucide-react';
import { aiApi } from '../api';
import BrandLogo, { LogoMark } from '../components/BrandLogo';
import { useAuth } from '../auth/AuthContext';
import { can, roleLabel } from '../auth/roles';

const NAV = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/patients', label: 'Patients', icon: Users },
  { to: '/admissions', label: 'Admissions', icon: Activity },
  { to: '/rooms', label: 'Rooms & Beds', icon: BedDouble },
  { to: '/providers', label: 'Doctors & Departments', icon: Stethoscope },
  { to: '/schedules', label: 'Doctor Schedules', icon: CalendarClock },
  { to: '/lab', label: 'Laboratory', icon: FlaskConical },
  { to: '/pharmacy', label: 'Pharmacy', icon: Pill },
  { to: '/bills', label: 'Billing', icon: Receipt, roles: can.billing },
  { to: '/claims', label: 'Insurance Claims', icon: FileCheck2, roles: can.billing },
  { to: '/payers', label: 'Payers', icon: ShieldCheck },
];

export default function AppLayout() {
  const { user, logout, hasRole } = useAuth();
  const [ai, setAi] = useState(null);

  useEffect(() => {
    aiApi.status().then(setAi).catch(() => setAi(null));
  }, []);

  return (
    <div className="flex min-h-screen">
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 flex-col border-r border-slate-200 bg-white md:flex">
        <div className="border-b border-slate-100 px-5 py-5">
          <BrandLogo />
          <p className="mt-3 flex items-center gap-2 truncate rounded-lg bg-slate-50 px-2.5 py-1.5 text-xs text-slate-600">
            <span className="h-2 w-2 shrink-0 rounded-full bg-emerald-500" />
            <span className="truncate">{user.hospitalName}</span>
          </p>
        </div>
        <nav className="flex-1 space-y-1 overflow-y-auto p-3">
          {NAV.filter((item) => hasRole(item.roles)).map(({ to, label, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition ${
                  isActive ? 'bg-blue-50 text-blue-700' : 'text-slate-600 hover:bg-slate-50'
                }`
              }
            >
              <Icon className="h-4 w-4" />
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-slate-100 p-4">
          {ai && (
            <p className={`mb-3 flex items-center gap-1 text-xs font-medium ${ai.enabled ? 'text-violet-700' : 'text-slate-400'}`}>
              <Sparkles className="h-3.5 w-3.5" /> {ai.enabled ? `Spring AI on (${ai.model})` : 'AI off - rule-based mode'}
            </p>
          )}
          <p className="text-sm font-semibold text-slate-800">{user.fullName}</p>
          <p className="text-xs text-slate-500">
            {user.username} - {roleLabel(user.role)}
          </p>
          <button
            type="button"
            onClick={logout}
            className="mt-3 flex w-full items-center justify-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50"
          >
            <LogOut className="h-4 w-4" /> Sign out
          </button>
        </div>
      </aside>

      <div className="flex-1 md:pl-64">
        <header className="sticky top-0 z-20 flex items-center justify-between border-b border-slate-200 bg-white/90 px-6 py-3 backdrop-blur md:hidden">
          <span className="flex items-center gap-2 font-black"><LogoMark size={28} /> AUSTRA<span className="-ml-2 text-blue-600">CARE</span></span>
          <button type="button" onClick={logout} className="text-sm text-slate-600">
            Sign out
          </button>
        </header>
        <main className="mx-auto max-w-7xl p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
