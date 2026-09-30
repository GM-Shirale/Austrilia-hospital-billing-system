import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { authApi } from '../api';
import { session } from '../api/client';
import { hasRole } from './roles';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => session.user);
  const navigate = useNavigate();

  const login = useCallback(async (tenantId, username, password) => {
    const response = await authApi.login(tenantId, username, password);
    session.save(response.accessToken, response.user);
    setUser(response.user);
    return response.user;
  }, []);

  /** Self-registration: the backend creates the user (BCrypt) and returns a JWT, so we are signed in. */
  const register = useCallback(async (form) => {
    const response = await authApi.register(form);
    session.save(response.accessToken, response.user);
    setUser(response.user);
    return response.user;
  }, []);

  const logout = useCallback(() => {
    // Revoke the JWT on the server (best effort), then forget it locally.
    authApi.logout().catch(() => {});
    session.clear();
    setUser(null);
    navigate('/login', { replace: true });
  }, [navigate]);

  // The axios interceptor fires this when the backend answers 401 (expired/invalid token).
  useEffect(() => {
    const onUnauthorized = () => {
      setUser(null);
      toast.error('Your session has expired. Please sign in again.');
      navigate('/login', { replace: true });
    };
    window.addEventListener('hb:unauthorized', onUnauthorized);
    return () => window.removeEventListener('hb:unauthorized', onUnauthorized);
  }, [navigate]);

  const value = useMemo(
    () => ({ user, login, register, logout, hasRole: (allowed) => hasRole(user, allowed) }),
    [user, login, register, logout],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
