const styles: Record<string, string> = {
  success: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  error: 'bg-rose-50 text-rose-700 ring-rose-600/20',
  warning: 'bg-amber-50 text-amber-700 ring-amber-600/20',
  neutral: 'bg-slate-100 text-slate-600 ring-slate-500/20',
  brand: 'bg-brand-50 text-brand-700 ring-brand-600/20',
}

export function Badge({ children, tone = 'neutral' }: { children: React.ReactNode; tone?: keyof typeof styles }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${styles[tone]}`}>
      {children}
    </span>
  )
}
