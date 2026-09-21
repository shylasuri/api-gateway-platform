import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import Layout from './components/Layout'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import ApiKeys from './pages/ApiKeys'
import Apis from './pages/Apis'
import Usage from './pages/Usage'
import Billing from './pages/Billing'
import Profile from './pages/Profile'
import AdminOverview from './pages/admin/AdminOverview'
import AdminApis from './pages/admin/AdminApis'
import AdminPlans from './pages/admin/AdminPlans'
import AdminConsumers from './pages/admin/AdminConsumers'
import AdminAnalytics from './pages/admin/AdminAnalytics'
import AdminBilling from './pages/admin/AdminBilling'

function RequireAuth({ children, role }: { children: JSX.Element; role?: 'ADMIN' | 'CONSUMER' }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (role && user.role !== role) return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/dashboard'} replace />
  return children
}

function AppRoutes() {
  const { user } = useAuth()

  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to={user.role === 'ADMIN' ? '/admin' : '/dashboard'} /> : <Login />} />
      <Route path="/register" element={user ? <Navigate to="/dashboard" /> : <Register />} />

      <Route element={<RequireAuth><Layout /></RequireAuth>}>
        <Route path="/dashboard" element={<RequireAuth role="CONSUMER"><Dashboard /></RequireAuth>} />
        <Route path="/api-keys" element={<RequireAuth role="CONSUMER"><ApiKeys /></RequireAuth>} />
        <Route path="/apis" element={<Apis />} />
        <Route path="/usage" element={<RequireAuth role="CONSUMER"><Usage /></RequireAuth>} />
        <Route path="/billing" element={<RequireAuth role="CONSUMER"><Billing /></RequireAuth>} />
        <Route path="/profile" element={<Profile />} />

        <Route path="/admin" element={<RequireAuth role="ADMIN"><AdminOverview /></RequireAuth>} />
        <Route path="/admin/apis" element={<RequireAuth role="ADMIN"><AdminApis /></RequireAuth>} />
        <Route path="/admin/plans" element={<RequireAuth role="ADMIN"><AdminPlans /></RequireAuth>} />
        <Route path="/admin/consumers" element={<RequireAuth role="ADMIN"><AdminConsumers /></RequireAuth>} />
        <Route path="/admin/analytics" element={<RequireAuth role="ADMIN"><AdminAnalytics /></RequireAuth>} />
        <Route path="/admin/billing" element={<RequireAuth role="ADMIN"><AdminBilling /></RequireAuth>} />
      </Route>

      <Route path="*" element={<Navigate to={user ? (user.role === 'ADMIN' ? '/admin' : '/dashboard') : '/login'} />} />
    </Routes>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  )
}
