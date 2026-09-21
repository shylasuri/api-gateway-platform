import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import { SubscriptionPlan } from '../../types'
import { StatCard, Card } from '../../components/Card'
import { Loading } from '../../components/Loading'
import { ErrorBanner } from '../../components/ErrorBanner'
import { Table } from '../../components/Table'

interface ConsumerUser { id: string; role: string }

export default function AdminBilling() {
  const [plans, setPlans] = useState<SubscriptionPlan[]>([])
  const [consumerCount, setConsumerCount] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    Promise.all([
      api.get<SubscriptionPlan[]>('/plans'),
      api.get<ConsumerUser[]>('/admin/consumers'),
    ])
      .then(([p, u]) => {
        setPlans(p.data)
        setConsumerCount(u.data.filter((x) => x.role === 'CONSUMER').length)
      })
      .catch((err) => setError(err.response?.data?.message ?? 'Failed to load billing overview'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <Loading />
  if (error) return <ErrorBanner message={error} />

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold text-slate-900">Billing overview</h1>
      <p className="text-sm text-slate-500">
        Aggregate revenue requires per-consumer billing snapshots; each consumer's own Billing page shows their
        real-time estimate calculated from actual usage. This page summarizes plan pricing across the platform.
      </p>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <StatCard label="Active consumers" value={consumerCount} />
        <StatCard label="Plans configured" value={plans.length} />
      </div>

      <Card>
        <Table headers={['Plan', 'Monthly price', 'Included requests', 'Overage price / request']}>
          {plans.map((p) => (
            <tr key={p.id}>
              <td className="px-4 py-3 font-medium text-slate-800">{p.name}</td>
              <td className="px-4 py-3">${p.monthlyPrice.toFixed(2)}</td>
              <td className="px-4 py-3">{p.monthlyRequestQuota.toLocaleString()}</td>
              <td className="px-4 py-3">${p.overagePricePerRequest.toFixed(4)}</td>
            </tr>
          ))}
        </Table>
      </Card>
    </div>
  )
}
