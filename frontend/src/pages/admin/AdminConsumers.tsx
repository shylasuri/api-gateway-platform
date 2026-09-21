import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import { Table } from '../../components/Table'
import { Badge } from '../../components/Badge'
import { Loading } from '../../components/Loading'
import { ErrorBanner } from '../../components/ErrorBanner'

interface ConsumerUser {
  id: string
  email: string
  fullName: string
  company: string
  role: string
  active: boolean
  createdAt: string
}

export default function AdminConsumers() {
  const [users, setUsers] = useState<ConsumerUser[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.get<ConsumerUser[]>('/admin/consumers')
      .then((res) => setUsers(res.data))
      .catch((err) => setError(err.response?.data?.message ?? 'Failed to load consumers'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <Loading />
  if (error) return <ErrorBanner message={error} />

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold text-slate-900">Consumers</h1>
      <Table headers={['Name', 'Email', 'Company', 'Role', 'Status', 'Joined']}>
        {users.map((u) => (
          <tr key={u.id}>
            <td className="px-4 py-3 font-medium text-slate-800">{u.fullName}</td>
            <td className="px-4 py-3 text-slate-500">{u.email}</td>
            <td className="px-4 py-3 text-slate-500">{u.company}</td>
            <td className="px-4 py-3"><Badge tone={u.role === 'ADMIN' ? 'brand' : 'neutral'}>{u.role}</Badge></td>
            <td className="px-4 py-3">{u.active ? <Badge tone="success">Active</Badge> : <Badge tone="error">Disabled</Badge>}</td>
            <td className="px-4 py-3 text-slate-500">{new Date(u.createdAt).toLocaleDateString()}</td>
          </tr>
        ))}
      </Table>
    </div>
  )
}
