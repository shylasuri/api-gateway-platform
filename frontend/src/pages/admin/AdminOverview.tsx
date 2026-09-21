import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import { AnalyticsResponse } from '../../types'
import { StatCard, Card } from '../../components/Card'
import { Loading } from '../../components/Loading'
import { ErrorBanner } from '../../components/ErrorBanner'
import { Table } from '../../components/Table'
import { EmptyState } from '../../components/EmptyState'

export default function AdminOverview() {
  const [analytics, setAnalytics] = useState<AnalyticsResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api
      .get<AnalyticsResponse>('/admin/analytics?days=30')
      .then((res) => setAnalytics(res.data))
      .catch((err) => setError(err.response?.data?.message ?? 'Failed to load analytics'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <Loading />
  if (error) return <ErrorBanner message={error} />
  if (!analytics) return null

  const errorRate = analytics.totalRequests > 0
    ? (analytics.rejectedRequests / analytics.totalRequests) * 100
    : 0

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold text-slate-900">Platform overview</h1>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <StatCard label="Total requests" value={analytics.totalRequests.toLocaleString()} />
        <StatCard label="Requests today" value={analytics.requestsToday.toLocaleString()} />
        <StatCard label="Rate-limit violations" value={analytics.rateLimitViolations.toLocaleString()} />
        <StatCard label="Quota violations" value={analytics.quotaViolations.toLocaleString()} />
        <StatCard label="Error rate" value={`${errorRate.toFixed(1)}%`} />
        <StatCard label="Avg latency" value={analytics.avgLatencyMs ? `${Math.round(analytics.avgLatencyMs)} ms` : '—'} />
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <p className="mb-4 text-sm font-medium text-slate-700">Top APIs (30d)</p>
          {analytics.topApis.length === 0 ? (
            <EmptyState title="No traffic yet" />
          ) : (
            <Table headers={['API', 'Requests']}>
              {analytics.topApis.map((row, i) => (
                <tr key={i}>
                  <td className="px-4 py-3">{String(row.apiName ?? row.field1 ?? '—')}</td>
                  <td className="px-4 py-3 font-medium">{String(row.count ?? row.field2 ?? 0)}</td>
                </tr>
              ))}
            </Table>
          )}
        </Card>

        <Card>
          <p className="mb-4 text-sm font-medium text-slate-700">Top consumers (30d)</p>
          {analytics.topConsumers.length === 0 ? (
            <EmptyState title="No traffic yet" />
          ) : (
            <Table headers={['Consumer', 'Requests']}>
              {analytics.topConsumers.map((row, i) => (
                <tr key={i}>
                  <td className="px-4 py-3">{String(row.email ?? row.field1 ?? '—')}</td>
                  <td className="px-4 py-3 font-medium">{String(row.count ?? row.field2 ?? 0)}</td>
                </tr>
              ))}
            </Table>
          )}
        </Card>
      </div>
    </div>
  )
}
