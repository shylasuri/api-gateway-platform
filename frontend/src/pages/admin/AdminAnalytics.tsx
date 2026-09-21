import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import { AnalyticsResponse } from '../../types'
import { StatCard, Card } from '../../components/Card'
import { Loading } from '../../components/Loading'
import { ErrorBanner } from '../../components/ErrorBanner'
import { EmptyState } from '../../components/EmptyState'
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'

const RANGES = [7, 14, 30, 90]

export default function AdminAnalytics() {
  const [days, setDays] = useState(30)
  const [analytics, setAnalytics] = useState<AnalyticsResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setLoading(true)
    api.get<AnalyticsResponse>(`/admin/analytics?days=${days}`)
      .then((res) => setAnalytics(res.data))
      .catch((err) => setError(err.response?.data?.message ?? 'Failed to load analytics'))
      .finally(() => setLoading(false))
  }, [days])

  if (loading) return <Loading />
  if (error) return <ErrorBanner message={error} />
  if (!analytics) return null

  const chartData = analytics.requestsOverTime.map((row) => ({
    date: String(row.date ?? row.field0 ?? ''),
    count: Number(row.count ?? row.field1 ?? 0),
  }))

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">Platform analytics</h1>
        <div className="flex gap-2">
          {RANGES.map((d) => (
            <button key={d} onClick={() => setDays(d)}
              className={`rounded-lg px-3 py-1.5 text-sm font-medium ${days === d ? 'bg-brand-600 text-white' : 'bg-white text-slate-600 ring-1 ring-slate-200'}`}>
              {d}d
            </button>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <StatCard label="Total requests" value={analytics.totalRequests.toLocaleString()} />
        <StatCard label="Successful" value={analytics.successfulRequests.toLocaleString()} />
        <StatCard label="Rate-limit violations" value={analytics.rateLimitViolations.toLocaleString()} />
        <StatCard label="Quota violations" value={analytics.quotaViolations.toLocaleString()} />
      </div>

      <Card>
        <p className="mb-4 text-sm font-medium text-slate-700">Traffic over time</p>
        {chartData.length === 0 ? <EmptyState title="No data" /> : (
          <ResponsiveContainer width="100%" height={280}>
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
    </div>
  )
}
