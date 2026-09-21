import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { AnalyticsResponse, UsageRecord } from '../types'
import { Card, StatCard } from '../components/Card'
import { Table } from '../components/Table'
import { Badge } from '../components/Badge'
import { Loading } from '../components/Loading'
import { ErrorBanner } from '../components/ErrorBanner'
import { EmptyState } from '../components/EmptyState'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'

const FILTERS = [
  { label: '7 days', value: 7 },
  { label: '14 days', value: 14 },
  { label: '30 days', value: 30 },
  { label: '90 days', value: 90 },
]

export default function Usage() {
  const [days, setDays] = useState(30)
  const [analytics, setAnalytics] = useState<AnalyticsResponse | null>(null)
  const [records, setRecords] = useState<UsageRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [statusFilter, setStatusFilter] = useState<'all' | 'allowed' | 'rejected'>('all')

  useEffect(() => {
    setLoading(true)
    Promise.all([
      api.get<AnalyticsResponse>(`/usage/analytics?days=${days}`),
      api.get<UsageRecord[]>('/usage'),
    ])
      .then(([a, u]) => {
        setAnalytics(a.data)
        setRecords(u.data)
      })
      .catch((err) => setError(err.response?.data?.message ?? 'Failed to load usage'))
      .finally(() => setLoading(false))
  }, [days])

  if (loading) return <Loading />
  if (error) return <ErrorBanner message={error} />
  if (!analytics) return null

  const chartData = analytics.requestsOverTime.map((row) => ({
    date: String(row.date ?? row.field0 ?? ''),
    count: Number(row.count ?? row.field1 ?? 0),
  }))

  const filtered = records.filter((r) => {
    if (statusFilter === 'allowed') return r.allowed
    if (statusFilter === 'rejected') return !r.allowed
    return true
  })

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-xl font-semibold text-slate-900">Usage</h1>
        <div className="flex gap-2">
          {FILTERS.map((f) => (
            <button
              key={f.value}
              onClick={() => setDays(f.value)}
              className={`rounded-lg px-3 py-1.5 text-sm font-medium ${
                days === f.value ? 'bg-brand-600 text-white' : 'bg-white text-slate-600 ring-1 ring-slate-200'
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <StatCard label="Total requests" value={analytics.totalRequests.toLocaleString()} />
        <StatCard label="Successful" value={analytics.successfulRequests.toLocaleString()} />
        <StatCard label="Rejected" value={analytics.rejectedRequests.toLocaleString()} />
        <StatCard label="Avg latency" value={analytics.avgLatencyMs ? `${Math.round(analytics.avgLatencyMs)} ms` : '—'} />
      </div>

      <Card>
        <p className="mb-4 text-sm font-medium text-slate-700">Requests per day</p>
        {chartData.length === 0 ? (
          <EmptyState title="No data for this range" />
        ) : (
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="date" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="count" fill="#4f46e5" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </Card>

      <Card>
        <div className="mb-4 flex items-center justify-between">
          <p className="text-sm font-medium text-slate-700">Request history</p>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as any)}
            className="rounded-lg border border-slate-300 px-2 py-1 text-sm"
          >
            <option value="all">All</option>
            <option value="allowed">Allowed</option>
            <option value="rejected">Rejected</option>
          </select>
        </div>
        {filtered.length === 0 ? (
          <EmptyState title="No requests match this filter" />
        ) : (
          <Table headers={['Time', 'Method', 'Endpoint', 'Status', 'Latency', 'Strategy']}>
            {filtered.map((r) => (
              <tr key={r.id}>
                <td className="px-4 py-3 text-slate-500">{new Date(r.requestedAt).toLocaleString()}</td>
                <td className="px-4 py-3 font-mono text-xs">{r.httpMethod}</td>
                <td className="px-4 py-3">{r.endpoint}</td>
                <td className="px-4 py-3">
                  {r.allowed ? (
                    <Badge tone="success">{r.responseStatus}</Badge>
                  ) : (
                    <Badge tone="error">{r.rejectionReason.replace('_', ' ')}</Badge>
                  )}
                </td>
                <td className="px-4 py-3 text-slate-500">{r.responseTimeMs ? `${r.responseTimeMs} ms` : '—'}</td>
                <td className="px-4 py-3 text-slate-500">{r.rateLimitStrategy ?? '—'}</td>
              </tr>
            ))}
          </Table>
        )}
      </Card>
    </div>
  )
}
