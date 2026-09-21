import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { BillingResponse } from '../types'
import { Card, StatCard } from '../components/Card'
import { Loading } from '../components/Loading'
import { ErrorBanner } from '../components/ErrorBanner'

export default function Billing() {
  const [billing, setBilling] = useState<BillingResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api
      .get<BillingResponse>('/billing')
      .then((res) => setBilling(res.data))
      .catch((err) => setError(err.response?.data?.message ?? 'Failed to load billing'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <Loading />
  if (error) return <ErrorBanner message={error} />
  if (!billing) return null

  const usagePct = billing.includedRequests > 0
    ? Math.min(100, (billing.actualRequests / billing.includedRequests) * 100)
    : 0

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold text-slate-900">Billing</h1>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <StatCard label="Current plan" value={billing.planName} />
        <StatCard label="Base price" value={`$${billing.monthlyPrice.toFixed(2)}`} />
        <StatCard label="Overage" value={`$${billing.overageAmount.toFixed(2)}`}
          sublabel={`${billing.overageRequests.toLocaleString()} requests over quota`} />
        <StatCard label="Estimated total" value={`$${billing.estimatedTotal.toFixed(2)}`} />
      </div>

      <Card>
        <p className="mb-2 text-sm font-medium text-slate-700">Quota usage this period</p>
        <p className="mb-3 text-xs text-slate-500">
          {billing.actualRequests.toLocaleString()} / {billing.includedRequests.toLocaleString()} included requests
        </p>
        <div className="h-2 w-full overflow-hidden rounded-full bg-slate-100">
          <div
            className={`h-full rounded-full ${usagePct >= 100 ? 'bg-rose-500' : 'bg-brand-600'}`}
            style={{ width: `${usagePct}%` }}
          />
        </div>
        <p className="mt-3 text-xs text-slate-400">
          Billing period: {billing.periodStart} – {billing.periodEnd}
        </p>
      </Card>

      <Card>
        <p className="mb-3 text-sm font-medium text-slate-700">Overage pricing</p>
        <p className="text-sm text-slate-500">
          ${billing.overagePricePerRequest.toFixed(4)} per request beyond your included quota.
        </p>
        {!billing.stripeCustomerId && (
          <p className="mt-3 rounded-lg bg-amber-50 p-3 text-xs text-amber-700">
            Stripe sync is not configured for this environment — amounts above are calculated directly from
            recorded usage, not from a live Stripe subscription. Set STRIPE_SECRET_KEY to enable Stripe sync.
          </p>
        )}
      </Card>
    </div>
  )
}
