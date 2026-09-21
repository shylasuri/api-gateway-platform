import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const consumerLinks = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/api-keys', label: 'API Keys' },
  { to: '/apis', label: 'APIs' },
  { to: '/usage', label: 'Usage' },
  { to: '/billing', label: 'Billing' },
  { to: '/profile', label: 'Profile' },
]

const adminLinks = [
  { to: '/admin', label: 'Overview' },
  { to: '/admin/apis', label: 'APIs' },
  { to: '/admin/plans', label: 'Plans' },
  { to: '/admin/consumers', label: 'Consumers' },
  { to: '/admin/analytics', label: 'Analytics' },
  { to: '/admin/billing', label: 'Billing' },
]

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const links = user?.role === 'ADMIN' ? adminLinks : consumerLinks

  return (
    <div className="flex min-h-screen bg-slate-50">
      <aside className="w-60 shrink-0 border-r border-slate-200 bg-white">
        <div className="flex h-16 items-center gap-2 border-b border-slate-200 px-5">
          <div className="h-7 w-7 rounded-md bg-brand-600" />
          <span className="font-semibold text-slate-900">GatewayPlatform</span>
        </div>
        <nav className="flex flex-col gap-1 p-3">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.to === '/admin'}
              className={({ isActive }) =>
                `rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-brand-50 text-brand-700'
                    : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
                }`
              }
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="flex flex-1 flex-col">
        <header className="flex h-16 items-center justify-between border-b border-slate-200 bg-white px-6">
          <div className="text-sm text-slate-500">
            {user?.role === 'ADMIN' ? 'Admin console' : 'Consumer dashboard'}
          </div>
          <div className="flex items-center gap-3">
            <span className="text-sm font-medium text-slate-700">{user?.fullName}</span>
            <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
              {user?.role}
            </span>
            <button
              onClick={() => {
                logout()
                navigate('/login')
              }}
              className="rounded-md border border-slate-200 px-3 py-1.5 text-sm font-medium text-slate-600 hover:bg-slate-50"
            >
              Log out
            </button>
          </div>
        </header>
        <main className="flex-1 p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
