import { useEffect, useMemo, useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { Building2, CheckCircle2, Circle, Loader2, Lock, Mail, User, UserPlus } from 'lucide-react';
import BrandLogo from '../components/BrandLogo';
import { authApi } from '../api';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { REGISTRATION_ROLES } from '../auth/roles';

const DEMO_USERS = [
  { username: 'admin', password: 'Admin@123', role: 'Admin' },
  { username: 'billing', password: 'Billing@123', role: 'Billing Officer' },
  { username: 'doctor', password: 'Doctor@123', role: 'Doctor' },
  { username: 'reception', password: 'Reception@123', role: 'Receptionist' },
  { username: 'lab', password: 'Lab@123', role: 'Lab' },
  { username: 'pharmacy', password: 'Pharmacy@123', role: 'Pharmacy' },
];

/* Same rules as the backend RegisterRequest (AuthDtos) so the user sees problems before submitting. */
const PASSWORD_RULES = [
  ['8+ characters', (p) => p.length >= 8],
  ['Upper-case letter', (p) => /[A-Z]/.test(p)],
  ['Lower-case letter', (p) => /[a-z]/.test(p)],
  ['Number', (p) => /\d/.test(p)],
  ['Symbol (e.g. @ # !)', (p) => /[^A-Za-z0-9]/.test(p)],
];
const USERNAME_RE = /^[A-Za-z0-9._-]{4,60}$/;
const NAME_RE = /^[\p{L}][\p{L} .'-]*$/u;
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

const EMPTY_REGISTRATION = { fullName: '', username: '', email: '', password: '', confirmPassword: '', role: 'RECEPTIONIST' };

function validateRegistration(form) {
  const errors = {};
  if (!form.fullName.trim() || form.fullName.trim().length < 2) errors.fullName = 'Enter your full name';
  else if (!NAME_RE.test(form.fullName.trim())) errors.fullName = 'Letters, spaces, apostrophes and hyphens only';
  if (!USERNAME_RE.test(form.username.trim())) errors.username = '4-60 characters: letters, digits, . _ -';
  if (!EMAIL_RE.test(form.email.trim())) errors.email = 'Enter a valid email address';
  if (!PASSWORD_RULES.every(([, test]) => test(form.password))) errors.password = 'Password does not meet the policy';
  if (form.confirmPassword !== form.password) errors.confirmPassword = 'Passwords do not match';
  if (!form.role) errors.role = 'Select a role';
  return errors;
}

export default function LoginPage({ initialMode = 'login' }) {
  const { user, login, register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [mode, setMode] = useState(initialMode);
  const [hospitals, setHospitals] = useState([]);
  const [tenantId, setTenantId] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [reg, setReg] = useState(EMPTY_REGISTRATION);
  const [touched, setTouched] = useState({});
  const [allowedRoles, setAllowedRoles] = useState(REGISTRATION_ROLES.map((r) => r.value));
  const [registrationEnabled, setRegistrationEnabled] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    authApi
      .hospitals()
      .then((list) => {
        setHospitals(list);
        if (list.length) setTenantId(list[0].id);
      })
      .catch((err) => setError(errorMessage(err)));
    authApi
      .registrationOptions()
      .then((opts) => {
        setRegistrationEnabled(opts.enabled);
        if (opts.roles?.length) setAllowedRoles(opts.roles);
      })
      .catch(() => {});
  }, []);

  useEffect(() => setMode(initialMode), [initialMode]);

  const regErrors = useMemo(() => validateRegistration(reg), [reg]);
  const roles = REGISTRATION_ROLES.filter((r) => allowedRoles.includes(r.value));

  if (user) return <Navigate to="/" replace />;

  const submitLogin = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(tenantId, username, password);
      navigate(location.state?.from || '/', { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const submitRegistration = async (e) => {
    e.preventDefault();
    setError('');
    setTouched({ fullName: true, username: true, email: true, password: true, confirmPassword: true, role: true });
    if (Object.keys(regErrors).length) return;
    setLoading(true);
    try {
      await register({
        ...reg,
        fullName: reg.fullName.trim(),
        username: reg.username.trim(),
        email: reg.email.trim(),
        hospitalId: tenantId,
      });
      navigate('/', { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const setField = (field) => (e) => setReg((r) => ({ ...r, [field]: e.target.value }));
  const blur = (field) => () => setTouched((t) => ({ ...t, [field]: true }));
  const fieldError = (field) => touched[field] && regErrors[field];

  const switchMode = (next) => {
    setMode(next);
    setError('');
    navigate(next === 'register' ? '/register' : '/login', { replace: true });
  };

  const hospitalSelect = (
    <label className="block">
      <span className="label">Hospital</span>
      <div className="relative">
        <Building2 className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
        <select className="input pl-9" value={tenantId} onChange={(e) => setTenantId(e.target.value)} required>
          {hospitals.map((h) => (
            <option key={h.id} value={h.id}>
              {h.name} ({h.hospitalType.toLowerCase()}, {h.state})
            </option>
          ))}
        </select>
      </div>
    </label>
  );

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-50 to-blue-50 p-4">
      <div className={`w-full ${mode === 'register' ? 'max-w-xl' : 'max-w-md'}`}>
        <div className="mb-6">
          <BrandLogo size="lg" subtitle="Hospital Billing & Clinical Engine - Medicare, PBS & private health claims" />
        </div>

        <div className="card overflow-hidden">
          <div className="grid grid-cols-2 border-b border-slate-200 text-sm font-semibold">
            <button
              type="button"
              onClick={() => switchMode('login')}
              className={`py-3 ${mode === 'login' ? 'border-b-2 border-blue-600 text-blue-700' : 'text-slate-500 hover:text-slate-700'}`}
            >
              Sign in
            </button>
            <button
              type="button"
              onClick={() => switchMode('register')}
              disabled={!registrationEnabled}
              className={`py-3 disabled:cursor-not-allowed disabled:text-slate-300 ${mode === 'register' ? 'border-b-2 border-blue-600 text-blue-700' : 'text-slate-500 hover:text-slate-700'}`}
            >
              Create account
            </button>
          </div>

          {mode === 'login' ? (
            <form onSubmit={submitLogin} className="space-y-4 p-7" aria-label="Sign in">
              {hospitalSelect}
              <label className="block">
                <span className="label">Username</span>
                <div className="relative">
                  <User className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                  <input className="input pl-9" name="username" value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required />
                </div>
              </label>
              <label className="block">
                <span className="label">Password</span>
                <div className="relative">
                  <Lock className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                  <input type="password" name="password" className="input pl-9" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required />
                </div>
              </label>
              {error && <p role="alert" className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</p>}
              <button
                type="submit"
                disabled={loading || !tenantId}
                className="flex w-full items-center justify-center gap-2 rounded-lg bg-blue-600 py-2.5 text-sm font-bold text-white hover:bg-blue-700 disabled:bg-blue-300"
              >
                {loading && <Loader2 className="h-4 w-4 animate-spin" />} Sign in
              </button>
              {registrationEnabled && (
                <p className="text-center text-sm text-slate-500">
                  New staff member? <Link to="/register" onClick={(e) => { e.preventDefault(); switchMode('register'); }} className="font-semibold text-blue-600 hover:underline">Create an account</Link>
                </p>
              )}
            </form>
          ) : (
            <form onSubmit={submitRegistration} noValidate className="space-y-4 p-7" aria-label="Create account">
              {hospitalSelect}
              <div className="grid gap-4 sm:grid-cols-2">
                <label className="block sm:col-span-2">
                  <span className="label">Full name <span className="text-rose-500">*</span></span>
                  <input className="input" name="fullName" value={reg.fullName} onChange={setField('fullName')} onBlur={blur('fullName')} autoComplete="name" placeholder="e.g. Olivia Bennett" />
                  {fieldError('fullName') && <span className="mt-1 block text-xs text-rose-600">{regErrors.fullName}</span>}
                </label>
                <label className="block">
                  <span className="label">Username <span className="text-rose-500">*</span></span>
                  <div className="relative">
                    <User className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                    <input className="input pl-9" name="newUsername" value={reg.username} onChange={setField('username')} onBlur={blur('username')} autoComplete="off" placeholder="olivia.bennett" />
                  </div>
                  {fieldError('username') && <span className="mt-1 block text-xs text-rose-600">{regErrors.username}</span>}
                </label>
                <label className="block">
                  <span className="label">Work email <span className="text-rose-500">*</span></span>
                  <div className="relative">
                    <Mail className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                    <input type="email" className="input pl-9" name="email" value={reg.email} onChange={setField('email')} onBlur={blur('email')} autoComplete="email" placeholder="name@hospital.com.au" />
                  </div>
                  {fieldError('email') && <span className="mt-1 block text-xs text-rose-600">{regErrors.email}</span>}
                </label>
                <label className="block">
                  <span className="label">Password <span className="text-rose-500">*</span></span>
                  <input type="password" className="input" name="newPassword" value={reg.password} onChange={setField('password')} onBlur={blur('password')} autoComplete="new-password" />
                </label>
                <label className="block">
                  <span className="label">Confirm password <span className="text-rose-500">*</span></span>
                  <input type="password" className="input" name="confirmPassword" value={reg.confirmPassword} onChange={setField('confirmPassword')} onBlur={blur('confirmPassword')} autoComplete="new-password" />
                  {fieldError('confirmPassword') && <span className="mt-1 block text-xs text-rose-600">{regErrors.confirmPassword}</span>}
                </label>
              </div>
              <ul className="grid grid-cols-2 gap-x-4 gap-y-1 text-xs sm:grid-cols-3">
                {PASSWORD_RULES.map(([text, test]) => {
                  const ok = test(reg.password);
                  return (
                    <li key={text} className={`flex items-center gap-1 ${ok ? 'text-emerald-600' : 'text-slate-400'}`}>
                      {ok ? <CheckCircle2 className="h-3.5 w-3.5" /> : <Circle className="h-3.5 w-3.5" />} {text}
                    </li>
                  );
                })}
              </ul>

              <fieldset>
                <legend className="label">Role <span className="text-rose-500">*</span></legend>
                <div className="grid gap-2 sm:grid-cols-2">
                  {roles.map((r) => (
                    <label
                      key={r.value}
                      className={`flex cursor-pointer gap-3 rounded-lg border p-3 text-sm ${reg.role === r.value ? 'border-blue-500 bg-blue-50 ring-1 ring-blue-500' : 'border-slate-200 hover:border-blue-300'}`}
                    >
                      <input type="radio" name="role" value={r.value} checked={reg.role === r.value} onChange={setField('role')} className="mt-0.5" />
                      <span>
                        <span className="block font-semibold text-slate-800">{r.label}</span>
                        <span className="block text-xs text-slate-500">{r.hint}</span>
                      </span>
                    </label>
                  ))}
                </div>
              </fieldset>

              {error && <p role="alert" className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</p>}
              <button
                type="submit"
                disabled={loading || !tenantId}
                className="flex w-full items-center justify-center gap-2 rounded-lg bg-blue-600 py-2.5 text-sm font-bold text-white hover:bg-blue-700 disabled:bg-blue-300"
              >
                {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <UserPlus className="h-4 w-4" />} Create account & sign in
              </button>
              <p className="text-center text-xs text-slate-400">Passwords are stored as BCrypt hashes. You are signed in with a JWT scoped to the selected hospital.</p>
            </form>
          )}
        </div>

        {mode === 'login' && (
          <div className="card mt-4 p-4">
            <p className="label">Demo accounts (Sydney Harbour Private) - click to fill</p>
            <div className="mt-2 grid grid-cols-3 gap-2">
              {DEMO_USERS.map((d) => (
                <button
                  key={d.username}
                  type="button"
                  onClick={() => {
                    setUsername(d.username);
                    setPassword(d.password);
                  }}
                  className="rounded-lg border border-slate-200 px-2 py-1.5 text-xs font-medium text-slate-600 hover:border-blue-300 hover:bg-blue-50"
                >
                  {d.role}
                </button>
              ))}
            </div>
            <p className="mt-2 text-xs text-slate-400">Melbourne General: melb.admin / Admin@123 (shows tenant isolation)</p>
          </div>
        )}
      </div>
    </div>
  );
}
