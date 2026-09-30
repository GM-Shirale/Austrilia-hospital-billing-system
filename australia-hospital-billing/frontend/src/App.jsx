import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './auth/ProtectedRoute';
import { can } from './auth/roles';
import AppLayout from './layout/AppLayout';
import { Spinner } from './components/ui/States';

// Each page is a separate chunk, downloaded the first time it is opened.
const LoginPage = lazy(() => import('./pages/LoginPage'));
const DashboardPage = lazy(() => import('./pages/DashboardPage'));
const PatientsPage = lazy(() => import('./pages/patients/PatientsPage'));
const PatientDetailPage = lazy(() => import('./pages/patients/PatientDetailPage'));
const AdmissionsPage = lazy(() => import('./pages/admissions/AdmissionsPage'));
const AdmissionDetailPage = lazy(() => import('./pages/admissions/AdmissionDetailPage'));
const RoomsPage = lazy(() => import('./pages/RoomsPage'));
const ProvidersPage = lazy(() => import('./pages/ProvidersPage'));
const SchedulesPage = lazy(() => import('./pages/SchedulesPage'));
const LabPage = lazy(() => import('./pages/LabPage'));
const PharmacyPage = lazy(() => import('./pages/PharmacyPage'));
const BillsPage = lazy(() => import('./pages/billing/BillsPage'));
const BillDetailPage = lazy(() => import('./pages/billing/BillDetailPage'));
const ClaimsPage = lazy(() => import('./pages/claims/ClaimsPage'));
const ClaimDetailPage = lazy(() => import('./pages/claims/ClaimDetailPage'));
const PayersPage = lazy(() => import('./pages/PayersPage'));

export default function App() {
  return (
    <Suspense fallback={<Spinner />}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<LoginPage initialMode="register" />} />
        <Route
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<DashboardPage />} />
          <Route path="patients" element={<PatientsPage />} />
          <Route path="patients/:id" element={<PatientDetailPage />} />
          <Route path="admissions" element={<AdmissionsPage />} />
          <Route path="admissions/:id" element={<AdmissionDetailPage />} />
          <Route path="rooms" element={<RoomsPage />} />
          <Route path="providers" element={<ProvidersPage />} />
          <Route path="schedules" element={<SchedulesPage />} />
          <Route path="lab" element={<LabPage />} />
          <Route path="pharmacy" element={<PharmacyPage />} />
          <Route path="payers" element={<PayersPage />} />
          <Route element={<ProtectedRoute roles={can.billing} />}>
            <Route path="bills" element={<BillsPage />} />
            <Route path="bills/:id" element={<BillDetailPage />} />
            <Route path="claims" element={<ClaimsPage />} />
            <Route path="claims/:id" element={<ClaimDetailPage />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}
