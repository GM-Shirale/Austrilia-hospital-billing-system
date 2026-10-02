import axios from 'axios';

const TOKEN_KEY = 'hb.token';
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const USER_KEY = 'hb.user';

export const session = {
  get token() {
    return localStorage.getItem(TOKEN_KEY);
  },
  get user() {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  },
  save(token, user) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  clear() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },
};

/**
 * Single axios instance for the whole app.
 * - Base URL: /api/v1 (proxied to Spring Boot by Vite in development).
 * - Adds the JWT and the X-Tenant-ID header to every request.
 * - Normalises RFC 7807 problem-details errors into { message, errorCode, details, status }.
 * - On 401 it clears the session and notifies the AuthContext (redirect to login).
 */
const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api/v1',
  headers: { 'Content-Type': 'application/json' },
  timeout: 20000,
});

client.interceptors.request.use((config) => {
  const token = session.token;
  const user = session.user;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  // Only a well-formed hospital id is sent; the backend takes the tenant from the JWT anyway.
  if (UUID_RE.test(user?.tenantId || '') && !config.headers['X-Tenant-ID']) config.headers['X-Tenant-ID'] = user.tenantId;
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const body = error.response?.data || {};
    const normalised = {
      status,
      errorCode: body.errorCode || (status ? `HTTP_${status}` : 'NETWORK_ERROR'),
      message:
        body.message ||
        body.detail ||
        (status ? `Request failed (${status})` : 'Cannot reach the server. Is the backend running on port 8081?'),
      details: Array.isArray(body.details) ? body.details : [],
      correlationId: body.correlationId,
    };
    if (status === 401 && !error.config?.url?.includes('/auth/')) {
      session.clear();
      window.dispatchEvent(new Event('hb:unauthorized'));
    }
    return Promise.reject(normalised);
  },
);

/** Turns the normalised error into one readable line (with field errors if present). */
export function errorMessage(err) {
  if (!err) return 'Unexpected error';
  if (err.details?.length) {
    const lines = err.details.map((d) => (typeof d === 'string' ? d : `${d.field}: ${d.message}`));
    return `${err.message}: ${lines.join('; ')}`;
  }
  return err.message || 'Unexpected error';
}

export default client;
