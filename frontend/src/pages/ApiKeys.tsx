import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { ApiKey } from '../types'
import { Table } from '../components/Table'
import { Badge } from '../components/Badge'
import { Loading } from '../components/Loading'
import { ErrorBanner } from '../components/ErrorBanner'
import { EmptyState } from '../components/EmptyState'
import { Modal } from '../components/Modal'

export default function ApiKeys() {
  const [keys, setKeys] = useState<ApiKey[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [label, setLabel] = useState('')
  const [newKey, setNewKey] = useState<ApiKey | null>(null)
  const [creating, setCreating] = useState(false)

  async function load() {
    setLoading(true)
    try {
      const res = await api.get<ApiKey[]>('/api-keys')
      setKeys(res.data)
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Failed to load API keys')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function createKey(e: React.FormEvent) {
    e.preventDefault()
    setCreating(true)
    try {
      const res = await api.post<ApiKey>('/api-keys', { label })
      setNewKey(res.data)
      setLabel('')
      await load()
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Failed to create API key')
    } finally {
      setCreating(false)
    }
  }

  async function revoke(id: string) {
    if (!confirm('Revoke this API key? This cannot be undone.')) return
    await api.delete(`/api-keys/${id}`)
    await load()
  }

  async function rotate(id: string) {
    const res = await api.post<ApiKey>(`/api-keys/${id}/rotate`)
    setNewKey(res.data)
    await load()
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">API Keys</h1>
      </div>

      {error && <ErrorBanner message={error} />}

      <form onSubmit={createKey} className="flex items-end gap-3 rounded-xl border border-slate-200 bg-white p-4">
        <div className="flex-1">
          <label className="mb-1 block text-sm font-medium text-slate-700">Label</label>
          <input
            required
            value={label}
            onChange={(e) => setLabel(e.target.value)}
            placeholder="e.g. Production server"
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
        </div>
        <button
          type="submit"
          disabled={creating}
          className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-60"
        >
          {creating ? 'Creating…' : 'Create key'}
        </button>
      </form>

      {loading ? (
        <Loading />
      ) : keys.length === 0 ? (
        <EmptyState title="No API keys yet" description="Create one above to start calling the gateway." />
      ) : (
        <Table headers={['Label', 'Key', 'Status', 'Created', 'Last used', 'Actions']}>
          {keys.map((k) => (
            <tr key={k.id}>
              <td className="px-4 py-3 font-medium text-slate-800">{k.label}</td>
              <td className="px-4 py-3 font-mono text-xs text-slate-500">{k.keyPrefix}</td>
              <td className="px-4 py-3">
                {k.revoked ? <Badge tone="error">Revoked</Badge> : <Badge tone="success">Active</Badge>}
              </td>
              <td className="px-4 py-3 text-slate-500">{new Date(k.createdAt).toLocaleDateString()}</td>
              <td className="px-4 py-3 text-slate-500">{k.lastUsedAt ? new Date(k.lastUsedAt).toLocaleString() : 'Never'}</td>
              <td className="px-4 py-3">
                {!k.revoked && (
                  <div className="flex gap-2">
                    <button onClick={() => rotate(k.id)} className="text-xs font-medium text-brand-600 hover:underline">
                      Rotate
                    </button>
                    <button onClick={() => revoke(k.id)} className="text-xs font-medium text-rose-600 hover:underline">
                      Revoke
                    </button>
                  </div>
                )}
              </td>
            </tr>
          ))}
        </Table>
      )}

      {newKey && (
        <Modal title="Your new API key" onClose={() => setNewKey(null)}>
          <p className="mb-3 text-sm text-slate-500">
            Copy this key now — for security it won't be shown again.
          </p>
          <code className="block break-all rounded-lg bg-slate-900 p-3 text-xs text-emerald-300">
            {newKey.rawKey}
          </code>
          <button
            onClick={() => {
              navigator.clipboard.writeText(newKey.rawKey ?? '')
            }}
            className="mt-4 w-full rounded-lg bg-brand-600 py-2 text-sm font-medium text-white hover:bg-brand-700"
          >
            Copy to clipboard
          </button>
        </Modal>
      )}
    </div>
  )
}
