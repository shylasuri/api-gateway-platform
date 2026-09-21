import { FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { AuthResponse } from '../types'
import { ErrorBanner } from '../components/ErrorBanner'

/** API-gateway routing mark: a hub with three routed nodes. */
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

function EyeIcon({ off }: { off?: boolean }) {
  return off ? (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.6"
      strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M9.9 4.24A9.12 9.12 0 0112 4c7 0 10 8 10 8a18.5 18.5 0 01-2.16 3.19M6.61 6.61A18.45 18.45 0 002 12s3 8 10 8a9.12 9.12 0 005.39-1.61" />
      <path d="M14.12 14.12a3 3 0 11-4.24-4.24" />
      <path d="M2 2l20 20" />
    </svg>
  ) : (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.6"
      strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M2 12s3-8 10-8 10 8 10 8-3 8-10 8-10-8-10-8z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  )
}

const inputClass =
  'w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-slate-900 ' +
  'placeholder-slate-400 shadow-sm transition-colors focus:border-brand-500 focus:outline-none ' +
  'focus:ring-2 focus:ring-brand-500/25'

export default function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  // Forgot-password flow (client-side; no self-service reset endpoint exists yet).
  const [mode, setMode] = useState<'signin' | 'forgot'>('signin')
  const [resetEmail, setResetEmail] = useState('')
  const [resetLoading, setResetLoading] = useState(false)
  const [resetSent, setResetSent] = useState(false)
  const [resetError, setResetError] = useState<string | null>(null)
  const [devResetUrl, setDevResetUrl] = useState<string | null>(null)

  const { login } = useAuth()
  const navigate = useNavigate()

  // --- authentication logic: unchanged ---
  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const res = await api.post<AuthResponse>('/auth/login', { email, password })
      login(res.data)
      navigate(res.data.role === 'ADMIN' ? '/admin' : '/dashboard')
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  async function handleReset(e: FormEvent) {
    e.preventDefault()
    setResetLoading(true)
    setResetError(null)
    try {
      const res = await api.post('/auth/forgot-password', { email: resetEmail })
      // In local/dev the backend returns the reset link (no email service).
      setDevResetUrl(res.data?.resetUrl ?? null)
      setResetSent(true)
    } catch (err: any) {
      setResetError(err.response?.data?.message ?? 'Could not send reset link. Please try again.')
    } finally {
      setResetLoading(false)
    }
  }

  function openForgot() {
    setResetEmail(email)
    setResetSent(false)
    setResetError(null)
    setDevResetUrl(null)
    setMode('forgot')
  }

  function backToSignIn() {
    setMode('signin')
    setResetSent(false)
    setResetError(null)
    setDevResetUrl(null)
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4 py-10">
      <div className="w-full max-w-md">
        <div className="rounded-2xl border border-slate-200 bg-white p-8 shadow-xl shadow-slate-900/5 sm:p-10">
          {/* Brand */}
          <div className="mb-8">
            <div className="flex items-center gap-3">
              <GatewayLogo />
              <div className="leading-tight">
                <div className="text-lg font-semibold tracking-tight text-slate-900">GatewayPlatform</div>
                <div className="text-xs font-medium uppercase tracking-wider text-slate-400">
                  API Management Platform
                </div>
              </div>
            </div>
          </div>

          {mode === 'signin' ? (
            <>
              <h1 className="text-2xl font-bold tracking-tight text-slate-900">Welcome back</h1>
              <p className="mt-1.5 text-sm text-slate-500">
                Sign in to access your APIs, analytics, and usage dashboard.
              </p>

              {error && (
                <div className="mt-6">
                  <ErrorBanner message={error} />
                </div>
              )}

              <form onSubmit={handleSubmit} className="mt-6 space-y-5">
                <div>
                  <label htmlFor="email" className="mb-1.5 block text-sm font-medium text-slate-700">
                    Email
                  </label>
                  <input
                    id="email"
                    type="email"
                    required
                    autoComplete="email"
                    placeholder="you@company.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className={inputClass}
                  />
                </div>

                <div>
                  <div className="mb-1.5 flex items-center justify-between">
                    <label htmlFor="password" className="block text-sm font-medium text-slate-700">
                      Password
                    </label>
                    <button
                      type="button"
                      onClick={openForgot}
                      className="text-sm font-medium text-brand-600 transition-colors hover:text-brand-700"
                    >
                      Forgot password?
                    </button>
                  </div>
                  <div className="relative">
                    <input
                      id="password"
                      type={showPassword ? 'text' : 'password'}
                      required
                      autoComplete="current-password"
                      placeholder="••••••••"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      className={inputClass + ' pr-11'}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword((s) => !s)}
                      aria-label={showPassword ? 'Hide password' : 'Show password'}
                      aria-pressed={showPassword}
                      className="absolute inset-y-0 right-0 flex items-center pr-3 text-slate-400 transition-colors hover:text-slate-600 focus:outline-none focus-visible:text-brand-600"
                    >
                      <EyeIcon off={showPassword} />
                    </button>
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  className="flex w-full items-center justify-center gap-2 rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-brand-700 focus:outline-none focus:ring-2 focus:ring-brand-500/40 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-70"
                >
                  {loading && <Spinner />}
                  {loading ? 'Signing in…' : 'Sign in'}
                </button>
              </form>

              <p className="mt-8 text-center text-sm text-slate-500">
                New to GatewayPlatform?{' '}
                <Link to="/register" className="font-semibold text-brand-600 transition-colors hover:text-brand-700">
                  Create an account
                </Link>
              </p>
            </>
          ) : (
            <>
              {resetSent ? (
                <div>
                  <div className="flex h-11 w-11 items-center justify-center rounded-full bg-emerald-50 text-emerald-600">
                    <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor"
                      strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                      <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
                      <path d="M22 4L12 14.01l-3-3" />
                    </svg>
                  </div>
                  <h1 className="mt-4 text-2xl font-bold tracking-tight text-slate-900">Check your email</h1>
                  <p className="mt-1.5 text-sm text-slate-500">
                    If an account is associated with{' '}
                    <span className="font-medium text-slate-700">{resetEmail}</span>, you&rsquo;ll receive a link to
                    reset your password shortly.
                  </p>

                  {devResetUrl && (
                    <div className="mt-5 rounded-lg border border-amber-200 bg-amber-50 p-3 text-xs text-amber-800">
                      <p className="font-semibold">Dev mode — no email service configured</p>
                      <p className="mt-1">Use this reset link directly:</p>
                      <a
                        href={devResetUrl}
                        className="mt-1 block break-all font-medium text-amber-900 underline underline-offset-2"
                      >
                        {devResetUrl}
                      </a>
                    </div>
                  )}

                  <button
                    type="button"
                    onClick={backToSignIn}
                    className="mt-8 flex w-full items-center justify-center rounded-lg border border-slate-300 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 shadow-sm transition-colors hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-brand-500/30"
                  >
                    Back to sign in
                  </button>
                </div>
              ) : (
                <>
                  <h1 className="text-2xl font-bold tracking-tight text-slate-900">Reset your password</h1>
                  <p className="mt-1.5 text-sm text-slate-500">
                    Enter the email tied to your account and we&rsquo;ll send you a link to reset your password.
                  </p>

                  {resetError && (
                    <div className="mt-6">
                      <ErrorBanner message={resetError} />
                    </div>
                  )}

                  <form onSubmit={handleReset} className="mt-6 space-y-5">
                    <div>
                      <label htmlFor="reset-email" className="mb-1.5 block text-sm font-medium text-slate-700">
                        Email
                      </label>
                      <input
                        id="reset-email"
                        type="email"
                        required
                        autoComplete="email"
                        placeholder="you@company.com"
                        value={resetEmail}
                        onChange={(e) => setResetEmail(e.target.value)}
                        className={inputClass}
                      />
                    </div>
                    <button
                      type="submit"
                      disabled={resetLoading}
                      className="flex w-full items-center justify-center gap-2 rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-brand-700 focus:outline-none focus:ring-2 focus:ring-brand-500/40 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-70"
                    >
                      {resetLoading && <Spinner />}
                      {resetLoading ? 'Sending link…' : 'Send reset link'}
                    </button>
                  </form>
                </>
              )}

              {!resetSent && (
                <button
                  type="button"
                  onClick={backToSignIn}
                  className="mt-8 flex w-full items-center justify-center gap-1.5 text-sm font-medium text-slate-500 transition-colors hover:text-slate-700"
                >
                  <svg viewBox="0 0 24 24" className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth="2"
                    strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <path d="M19 12H5" />
                    <path d="M12 19l-7-7 7-7" />
                  </svg>
                  Back to sign in
                </button>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  )
}
