import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import { BackendApi } from '../../types'
import { Table } from '../../components/Table'
import { Badge } from '../../components/Badge'
import { Loading } from '../../components/Loading'
import { ErrorBanner } from '../../components/ErrorBanner'
import { Modal } from '../../components/Modal'

const STRATEGIES = ['FIXED_WINDOW', 'SLIDING_WINDOW', 'TOKEN_BUCKET', 'LEAKY_BUCKET']

export default function AdminApis() {
  const [apis, setApis] = useState<BackendApi[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showCreate, setShowCreate] = useState(false)
  const [rateLimitTarget, setRateLimitTarget] = useState<BackendApi | null>(null)

  const [form, setForm] = useState({
    name: '', description: '', gatewayRoute: '', backendUrl: '', httpMethod: '',
  })
  const [rlForm, setRlForm] = useState({
    strategy: 'FIXED_WINDOW', limitCount: 100, windowSeconds: 60,
    bucketCapacity: 100, refillRate: 10, queueCapacity: 100, processingRate: 10,
  })

  async function load() {
    setLoading(true)
    try {
      const res = await api.get<BackendApi[]>('/admin/apis')
      setApis(res.data)
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Failed to load APIs')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function createApi(e: React.FormEvent) {
    e.preventDefault()
    try {
      await api.post('/admin/apis', { ...form, httpMethod: form.httpMethod || null, active: true })
      setShowCreate(false)
      setForm({ name: '', description: '', gatewayRoute: '', backendUrl: '', httpMethod: '' })
      await load()
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Failed to create API')
    }
  }

  async function toggleActive(a: BackendApi) {
    await api.put(`/admin/apis/${a.id}`, { active: !a.active })
    await load()
  }

  async function deleteApi(id: string) {
    if (!confirm('Delete this API? This cannot be undone.')) return
    await api.delete(`/admin/apis/${id}`)
    await load()
  }

  async function saveRateLimit(e: React.FormEvent) {
    e.preventDefault()
    if (!rateLimitTarget) return
    await api.put(`/admin/apis/${rateLimitTarget.id}/rate-limit`, rlForm)
    setRateLimitTarget(null)
  }

  if (loading) return <Loading />

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">Registered APIs</h1>
        <button onClick={() => setShowCreate(true)} className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700">
          + Register API
        </button>
      </div>

      {error && <ErrorBanner message={error} />}

      <Table headers={['Name', 'Route', 'Backend URL', 'Status', 'Actions']}>
        {apis.map((a) => (
          <tr key={a.id}>
            <td className="px-4 py-3 font-medium text-slate-800">{a.name}</td>
            <td className="px-4 py-3 font-mono text-xs">{a.gatewayRoute}</td>
            <td className="px-4 py-3 font-mono text-xs text-slate-500">{a.backendUrl}</td>
            <td className="px-4 py-3">{a.active ? <Badge tone="success">Active</Badge> : <Badge tone="neutral">Inactive</Badge>}</td>
            <td className="px-4 py-3">
              <div className="flex gap-3">
                <button onClick={() => setRateLimitTarget(a)} className="text-xs font-medium text-brand-600 hover:underline">Rate limit</button>
                <button onClick={() => toggleActive(a)} className="text-xs font-medium text-slate-600 hover:underline">
                  {a.active ? 'Deactivate' : 'Activate'}
                </button>
                <button onClick={() => deleteApi(a.id)} className="text-xs font-medium text-rose-600 hover:underline">Delete</button>
              </div>
            </td>
          </tr>
        ))}
      </Table>

      {showCreate && (
        <Modal title="Register a backend API" onClose={() => setShowCreate(false)}>
          <form onSubmit={createApi} className="space-y-3">
            <Field label="Name"><input required className="input" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
            <Field label="Description"><input className="input" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field>
            <Field label="Gateway route"><input required placeholder="/gateway/orders" className="input" value={form.gatewayRoute} onChange={(e) => setForm({ ...form, gatewayRoute: e.target.value })} /></Field>
            <Field label="Backend URL"><input required placeholder="http://orders-service:8082/orders" className="input" value={form.backendUrl} onChange={(e) => setForm({ ...form, backendUrl: e.target.value })} /></Field>
            <Field label="HTTP method (blank = any)"><input placeholder="GET" className="input" value={form.httpMethod} onChange={(e) => setForm({ ...form, httpMethod: e.target.value })} /></Field>
            <button type="submit" className="w-full rounded-lg bg-brand-600 py-2 text-sm font-medium text-white hover:bg-brand-700">Register</button>
          </form>
        </Modal>
      )}

      {rateLimitTarget && (
        <Modal title={`Rate limit — ${rateLimitTarget.name}`} onClose={() => setRateLimitTarget(null)}>
          <form onSubmit={saveRateLimit} className="space-y-3">
            <Field label="Strategy">
              <select className="input" value={rlForm.strategy} onChange={(e) => setRlForm({ ...rlForm, strategy: e.target.value })}>
                {STRATEGIES.map((s) => <option key={s} value={s}>{s}</option>)}
              </select>
            </Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Limit / bucket cap"><input type="number" className="input" value={rlForm.limitCount} onChange={(e) => setRlForm({ ...rlForm, limitCount: +e.target.value, bucketCapacity: +e.target.value, queueCapacity: +e.target.value })} /></Field>
              <Field label="Window seconds"><input type="number" className="input" value={rlForm.windowSeconds} onChange={(e) => setRlForm({ ...rlForm, windowSeconds: +e.target.value })} /></Field>
              <Field label="Refill rate (token/s)"><input type="number" className="input" value={rlForm.refillRate} onChange={(e) => setRlForm({ ...rlForm, refillRate: +e.target.value })} /></Field>
              <Field label="Processing rate (leak/s)"><input type="number" className="input" value={rlForm.processingRate} onChange={(e) => setRlForm({ ...rlForm, processingRate: +e.target.value })} /></Field>
            </div>
            <button type="submit" className="w-full rounded-lg bg-brand-600 py-2 text-sm font-medium text-white hover:bg-brand-700">Save</button>
          </form>
        </Modal>
      )}
    </div>
  )
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <label className="mb-1 block text-sm font-medium text-slate-700">{label}</label>
      {children}
    </div>
  )
}
