import { FormEvent, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../api/client'
import { ErrorBanner } from '../components/ErrorBanner'

function GatewayLogo() {
  return (
    <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-600 shadow-sm shadow-brand-600/30">
      <svg viewBox="0 0 40 40" className="h-6 w-6" fill="none" aria-hidden="true">
        <g stroke="white" strokeWidth="2" strokeLinecap="round">
          <path d="M20 20 L11 12" />
          <path d="M20 20 L11 28" />
          <path d="M20 20 L30 20" />
        </g>
        <g fill="white">
          <circle cx="20" cy="20" r="3.2" />
          <circle cx="11" cy="12" r="2.3" />
          <circle cx="11" cy="28" r="2.3" />
          <circle cx="30" cy="20" r="2.3" />
        </g>
      </svg>
    </div>
  )
}

function Spinner() {
  return (
    <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-90" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.4 0 0 5.4 0 12h4z" />
    </svg>
  )
}

const inputClass =
  'w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-slate-900 ' +
  'placeholder-slate-400 shadow-sm transition-colors focus:border-brand-500 focus:outline-none ' +
  'focus:ring-2 focus:ring-brand-500/25'

export default function ResetPassword() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''

  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [show, setShow] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [done, setDone] = useState(false)
  const navigate = useNavigate()

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (password.length < 8) {
      setError('Password must be at least 8 characters.')
      return
    }
    if (password !== confirm) {
      setError('Passwords do not match.')
      return
    }
    setLoading(true)
    try {
      await api.post('/auth/reset-password', { token, newPassword: password })
      setDone(true)
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Could not reset your password. The link may have expired.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4 py-10">
      <div className="w-full max-w-md">
        <div className="rounded-2xl border border-slate-200 bg-white p-8 shadow-xl shadow-slate-900/5 sm:p-10">
          <div className="mb-8 flex items-center gap-3">
            <GatewayLogo />
            <div className="leading-tight">
              <div className="text-lg font-semibold tracking-tight text-slate-900">GatewayPlatform</div>
              <div className="text-xs font-medium uppercase tracking-wider text-slate-400">
                API Management Platform
              </div>
            </div>
          </div>

          {done ? (
            <div>
              <div className="flex h-11 w-11 items-center justify-center rounded-full bg-emerald-50 text-emerald-600">
                <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="2"
                  strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
                  <path d="M22 4L12 14.01l-3-3" />
                </svg>
              </div>
              <h1 className="mt-4 text-2xl font-bold tracking-tight text-slate-900">Password updated</h1>
              <p className="mt-1.5 text-sm text-slate-500">
                Your password has been reset. You can now sign in with your new password.
              </p>
              <button
                type="button"
                onClick={() => navigate('/login')}
                className="mt-8 flex w-full items-center justify-center rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-brand-700 focus:outline-none focus:ring-2 focus:ring-brand-500/40 focus:ring-offset-2"
              >
                Continue to sign in
              </button>
            </div>
          ) : !token ? (
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-slate-900">Invalid reset link</h1>
              <p className="mt-1.5 text-sm text-slate-500">
                This link is missing its reset token. Request a new password reset from the sign-in page.
              </p>
              <Link
                to="/login"
                className="mt-8 flex w-full items-center justify-center rounded-lg border border-slate-300 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 shadow-sm transition-colors hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-brand-500/30"
              >
                Back to sign in
              </Link>
            </div>
          ) : (
            <>
              <h1 className="text-2xl font-bold tracking-tight text-slate-900">Set a new password</h1>
              <p className="mt-1.5 text-sm text-slate-500">Choose a strong password you don&rsquo;t use elsewhere.</p>

              {error && (
                <div className="mt-6">
                  <ErrorBanner message={error} />
                </div>
              )}

              <form onSubmit={handleSubmit} className="mt-6 space-y-5">
                <div>
                  <label htmlFor="new-password" className="mb-1.5 block text-sm font-medium text-slate-700">
                    New password
                  </label>
                  <input
                    id="new-password"
                    type={show ? 'text' : 'password'}
                    required
                    minLength={8}
                    autoComplete="new-password"
                    placeholder="At least 8 characters"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className={inputClass}
                  />
                </div>
                <div>
                  <label htmlFor="confirm-password" className="mb-1.5 block text-sm font-medium text-slate-700">
                    Confirm password
                  </label>
                  <input
                    id="confirm-password"
                    type={show ? 'text' : 'password'}
                    required
                    autoComplete="new-password"
                    placeholder="Re-enter your new password"
                    value={confirm}
                    onChange={(e) => setConfirm(e.target.value)}
                    className={inputClass}
                  />
                </div>
                <label className="flex items-center gap-2 text-sm text-slate-500">
                  <input
                    type="checkbox"
                    checked={show}
                    onChange={(e) => setShow(e.target.checked)}
                    className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500/40"
                  />
                  Show password
                </label>
                <button
                  type="submit"
                  disabled={loading}
                  className="flex w-full items-center justify-center gap-2 rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-brand-700 focus:outline-none focus:ring-2 focus:ring-brand-500/40 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-70"
                >
                  {loading && <Spinner />}
                  {loading ? 'Updating…' : 'Reset password'}
                </button>
              </form>

              <p className="mt-8 text-center text-sm text-slate-500">
                Remembered it?{' '}
                <Link to="/login" className="font-semibold text-brand-600 transition-colors hover:text-brand-700">
                  Back to sign in
                </Link>
              </p>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
