import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';
import { useAuth } from './AuthContext';

/** Redirects anonymous users to /login and shows "no access" when the role is not allowed. */
export default function ProtectedRoute({ roles, children }) {
  const { user, hasRole } = useAuth();
  const location = useLocation();

  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (roles && !hasRole(roles)) {
    return (
      <div className="card mx-auto mt-16 max-w-md p-8 text-center">
        <ShieldAlert className="mx-auto h-10 w-10 text-amber-500" />
        <h2 className="mt-3 text-lg font-bold">No access</h2>
        <p className="mt-1 text-sm text-slate-500">Your role ({user.role}) cannot open this page.</p>
      </div>
    );
  }
  return children ?? <Outlet />;
}
