import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { AnalyticsResponse, DashboardSummary, UsageRecord } from '../types'
import { StatCard, Card } from '../components/Card'
import { Loading } from '../components/Loading'
import { ErrorBanner } from '../components/ErrorBanner'
import { Badge } from '../components/Badge'
import { EmptyState } from '../components/EmptyState'
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'

export default function Dashboard() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [analytics, setAnalytics] = useState<AnalyticsResponse | null>(null)
  const [recent, setRecent] = useState<UsageRecord[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  async function load() {
    try {
      const [s, a, u] = await Promise.all([
        api.get<DashboardSummary>('/dashboard/summary'),
        api.get<AnalyticsResponse>('/usage/analytics?days=14'),
        api.get<UsageRecord[]>('/usage'),
      ])
      setSummary(s.data)
      setAnalytics(a.data)
      setRecent(u.data.slice(0, 8))
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Failed to load dashboard')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    const interval = setInterval(load, 15000) // simple polling for "real-time" usage
    return () => clearInterval(interval)
  }, [])

  if (loading) return <Loading label="Loading dashboard…" />
  if (error) return <ErrorBanner message={error} />
  if (!summary) return null

  const chartData = (analytics?.requestsOverTime ?? []).map((row) => ({
    date: String(row.date ?? row.field0 ?? ''),
    count: Number(row.count ?? row.field1 ?? 0),
  }))

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">Dashboard</h1>
          <p className="text-sm text-slate-500">Current plan: <Badge tone="brand">{summary.planName}</Badge></p>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <StatCard label="Requests today" value={summary.requestsToday.toLocaleString()}
          sublabel={summary.dailyQuota >= 0 ? `of ${summary.dailyQuota.toLocaleString()} daily quota` : 'no daily cap'} />
        <StatCard label="Monthly usage" value={summary.requestsThisMonth.toLocaleString()}
          sublabel={`of ${summary.monthlyQuota.toLocaleString()} included`} />
        <StatCard label="Remaining quota" value={summary.remainingQuota.toLocaleString()} />
        <StatCard label="Rate limit" value={`${summary.rateLimit}/${summary.rateLimitWindowSeconds}s`} />
        <StatCard label="Success rate" value={`${summary.successRate.toFixed(1)}%`} />
        <StatCard label="Error rate" value={`${summary.errorRate.toFixed(1)}%`} />
        <StatCard label="Avg latency" value={summary.avgLatencyMs ? `${Math.round(summary.avgLatencyMs)} ms` : '—'} />
      </div>

      <Card>
        <p className="mb-4 text-sm font-medium text-slate-700">Requests over the last 14 days</p>
        {chartData.length === 0 ? (
          <EmptyState title="No usage yet" description="Make a request through your API key to see traffic here." />
        ) : (
          <ResponsiveContainer width="100%" height={240}>
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="date" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
              <Tooltip />
              <Line type="monotone" dataKey="count" stroke="#4f46e5" strokeWidth={2} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        )}
      </Card>

      <Card>
        <p className="mb-4 text-sm font-medium text-slate-700">Recent requests</p>
        {recent.length === 0 ? (
          <EmptyState title="No requests yet" description="Recent gateway calls will show up here." />
        ) : (
          <div className="space-y-2">
            {recent.map((r) => (
              <div key={r.id} className="flex items-center justify-between border-b border-slate-100 py-2 text-sm last:border-0">
                <div className="flex items-center gap-3">
                  <span className="font-mono text-xs text-slate-400">{r.httpMethod}</span>
                  <span className="text-slate-700">{r.endpoint}</span>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-xs text-slate-400">{new Date(r.requestedAt).toLocaleTimeString()}</span>
                  {r.allowed ? (
                    <Badge tone="success">{r.responseStatus ?? 'OK'}</Badge>
                  ) : (
                    <Badge tone="error">{r.rejectionReason.replace('_', ' ')}</Badge>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  )
}
