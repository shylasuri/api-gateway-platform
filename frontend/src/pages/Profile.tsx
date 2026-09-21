import { useAuth } from '../context/AuthContext'
import { Card } from '../components/Card'

export default function Profile() {
  const { user } = useAuth()
  if (!user) return null

  return (
    <div className="max-w-lg space-y-6">
      <h1 className="text-xl font-semibold text-slate-900">Profile</h1>
      <Card>
        <dl className="space-y-4 text-sm">
          <div className="flex justify-between border-b border-slate-100 pb-3">
            <dt className="text-slate-500">Full name</dt>
            <dd className="font-medium text-slate-800">{user.fullName}</dd>
          </div>
          <div className="flex justify-between border-b border-slate-100 pb-3">
            <dt className="text-slate-500">Email</dt>
            <dd className="font-medium text-slate-800">{user.email}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-slate-500">Role</dt>
            <dd className="font-medium text-slate-800">{user.role}</dd>
          </div>
        </dl>
      </Card>
    </div>
  )
}
