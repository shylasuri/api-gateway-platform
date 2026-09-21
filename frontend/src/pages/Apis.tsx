import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { BackendApi } from '../types'
import { Table } from '../components/Table'
import { Badge } from '../components/Badge'
import { Loading } from '../components/Loading'
import { ErrorBanner } from '../components/ErrorBanner'
import { EmptyState } from '../components/EmptyState'

export default function Apis() {
  const [apis, setApis] = useState<BackendApi[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api
      .get<BackendApi[]>('/apis')
      .then((res) => setApis(res.data))
      .catch((err) => setError(err.response?.data?.message ?? 'Failed to load APIs'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <Loading />
  if (error) return <ErrorBanner message={error} />

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold text-slate-900">Available APIs</h1>
      {apis.length === 0 ? (
        <EmptyState title="No APIs registered yet" description="Ask an admin to register a backend API." />
      ) : (
        <Table headers={['Name', 'Route', 'Method', 'Status', 'Description']}>
          {apis.map((a) => (
            <tr key={a.id}>
              <td className="px-4 py-3 font-medium text-slate-800">{a.name}</td>
              <td className="px-4 py-3 font-mono text-xs text-slate-500">{a.gatewayRoute}</td>
              <td className="px-4 py-3 text-slate-500">{a.httpMethod ?? 'ANY'}</td>
              <td className="px-4 py-3">
                {a.active ? <Badge tone="success">Active</Badge> : <Badge tone="neutral">Inactive</Badge>}
              </td>
              <td className="px-4 py-3 text-slate-500">{a.description}</td>
            </tr>
          ))}
        </Table>
      )}
    </div>
  )
}
