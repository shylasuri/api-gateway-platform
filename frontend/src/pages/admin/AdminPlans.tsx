import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import { SubscriptionPlan } from '../../types'
import { Table } from '../../components/Table'
import { Loading } from '../../components/Loading'
import { ErrorBanner } from '../../components/ErrorBanner'
import { Modal } from '../../components/Modal'

const STRATEGIES = ['FIXED_WINDOW', 'SLIDING_WINDOW', 'TOKEN_BUCKET', 'LEAKY_BUCKET']

const emptyForm = {
  name: '', monthlyPrice: 0, monthlyRequestQuota: 5000, dailyRequestQuota: '' as number | '',
  rateLimit: 100, rateLimitWindowSeconds: 60, rateLimitStrategy: 'FIXED_WINDOW',
  bucketCapacity: '' as number | '', refillRate: '' as number | '',
  queueCapacity: '' as number | '', processingRate: '' as number | '',
  overagePricePerRequest: 0.01,
}

export default function AdminPlans() {
  const [plans, setPlans] = useState<SubscriptionPlan[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [editing, setEditing] = useState<SubscriptionPlan | null>(null)
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState(emptyForm)

  async function load() {
    setLoading(true)
    try {
      const res = await api.get<SubscriptionPlan[]>('/plans')
      setPlans(res.data)
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Failed to load plans')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  function openEdit(p: SubscriptionPlan) {
    setEditing(p)
    setForm({
      name: p.name, monthlyPrice: p.monthlyPrice, monthlyRequestQuota: p.monthlyRequestQuota,
      dailyRequestQuota: p.dailyRequestQuota ?? '', rateLimit: p.rateLimit,
      rateLimitWindowSeconds: p.rateLimitWindowSeconds, rateLimitStrategy: p.rateLimitStrategy,
      bucketCapacity: p.bucketCapacity ?? '', refillRate: p.refillRate ?? '',
      queueCapacity: p.queueCapacity ?? '', processingRate: p.processingRate ?? '',
      overagePricePerRequest: p.overagePricePerRequest,
    })
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    const payload = {
      ...form,
      dailyRequestQuota: form.dailyRequestQuota === '' ? null : Number(form.dailyRequestQuota),
      bucketCapacity: form.bucketCapacity === '' ? null : Number(form.bucketCapacity),
      refillRate: form.refillRate === '' ? null : Number(form.refillRate),
      queueCapacity: form.queueCapacity === '' ? null : Number(form.queueCapacity),
      processingRate: form.processingRate === '' ? null : Number(form.processingRate),
    }
    try {
      if (editing) {
        await api.put(`/admin/plans/${editing.id}`, payload)
      } else {
        await api.post('/admin/plans', payload)
      }
      setEditing(null)
      setCreating(false)
      setForm(emptyForm)
      await load()
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Failed to save plan')
    }
  }

  if (loading) return <Loading />

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">Subscription Plans</h1>
        <button onClick={() => setCreating(true)} className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700">
          + New plan
        </button>
      </div>

      {error && <ErrorBanner message={error} />}

      <Table headers={['Plan', 'Price', 'Monthly quota', 'Rate limit', 'Strategy', 'Overage price', '']}>
        {plans.map((p) => (
          <tr key={p.id}>
            <td className="px-4 py-3 font-medium text-slate-800">{p.name}</td>
            <td className="px-4 py-3">${p.monthlyPrice.toFixed(2)}</td>
            <td className="px-4 py-3">{p.monthlyRequestQuota.toLocaleString()}</td>
            <td className="px-4 py-3">{p.rateLimit}/{p.rateLimitWindowSeconds}s</td>
            <td className="px-4 py-3 text-xs">{p.rateLimitStrategy}</td>
            <td className="px-4 py-3">${p.overagePricePerRequest.toFixed(4)}</td>
            <td className="px-4 py-3">
              <button onClick={() => openEdit(p)} className="text-xs font-medium text-brand-600 hover:underline">Edit</button>
            </td>
          </tr>
        ))}
      </Table>

      {(editing || creating) && (
        <Modal title={editing ? `Edit ${editing.name}` : 'New plan'} onClose={() => { setEditing(null); setCreating(false); setForm(emptyForm) }}>
          <form onSubmit={submit} className="max-h-[70vh] space-y-3 overflow-y-auto pr-1">
            <Field label="Name"><input required className="input" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Monthly price ($)"><input type="number" step="0.01" className="input" value={form.monthlyPrice} onChange={(e) => setForm({ ...form, monthlyPrice: +e.target.value })} /></Field>
              <Field label="Overage price / request ($)"><input type="number" step="0.0001" className="input" value={form.overagePricePerRequest} onChange={(e) => setForm({ ...form, overagePricePerRequest: +e.target.value })} /></Field>
              <Field label="Monthly quota"><input type="number" className="input" value={form.monthlyRequestQuota} onChange={(e) => setForm({ ...form, monthlyRequestQuota: +e.target.value })} /></Field>
              <Field label="Daily quota (blank = none)"><input type="number" className="input" value={form.dailyRequestQuota} onChange={(e) => setForm({ ...form, dailyRequestQuota: e.target.value === '' ? '' : +e.target.value })} /></Field>
              <Field label="Rate limit"><input type="number" className="input" value={form.rateLimit} onChange={(e) => setForm({ ...form, rateLimit: +e.target.value })} /></Field>
              <Field label="Window seconds"><input type="number" className="input" value={form.rateLimitWindowSeconds} onChange={(e) => setForm({ ...form, rateLimitWindowSeconds: +e.target.value })} /></Field>
            </div>
            <Field label="Rate limit strategy">
              <select className="input" value={form.rateLimitStrategy} onChange={(e) => setForm({ ...form, rateLimitStrategy: e.target.value })}>
                {STRATEGIES.map((s) => <option key={s} value={s}>{s}</option>)}
              </select>
            </Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Bucket capacity"><input type="number" className="input" value={form.bucketCapacity} onChange={(e) => setForm({ ...form, bucketCapacity: e.target.value === '' ? '' : +e.target.value })} /></Field>
              <Field label="Refill rate"><input type="number" className="input" value={form.refillRate} onChange={(e) => setForm({ ...form, refillRate: e.target.value === '' ? '' : +e.target.value })} /></Field>
              <Field label="Queue capacity"><input type="number" className="input" value={form.queueCapacity} onChange={(e) => setForm({ ...form, queueCapacity: e.target.value === '' ? '' : +e.target.value })} /></Field>
              <Field label="Processing rate"><input type="number" className="input" value={form.processingRate} onChange={(e) => setForm({ ...form, processingRate: e.target.value === '' ? '' : +e.target.value })} /></Field>
            </div>
            <button type="submit" className="w-full rounded-lg bg-brand-600 py-2 text-sm font-medium text-white hover:bg-brand-700">Save plan</button>
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
