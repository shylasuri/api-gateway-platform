export interface AuthResponse {
  token: string
  userId: string
  email: string
  fullName: string
  role: 'CONSUMER' | 'ADMIN'
}

export interface ApiKey {
  id: string
  label: string
  keyPrefix: string
  revoked: boolean
  createdAt: string
  lastUsedAt: string | null
  rawKey: string | null
}

export interface BackendApi {
  id: string
  name: string
  description: string
  gatewayRoute: string
  backendUrl: string
  httpMethod: string | null
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface SubscriptionPlan {
  id: string
  name: string
  monthlyPrice: number
  monthlyRequestQuota: number
  dailyRequestQuota: number | null
  rateLimit: number
  rateLimitWindowSeconds: number
  rateLimitStrategy: 'FIXED_WINDOW' | 'SLIDING_WINDOW' | 'TOKEN_BUCKET' | 'LEAKY_BUCKET'
  bucketCapacity: number | null
  refillRate: number | null
  queueCapacity: number | null
  processingRate: number | null
  overagePricePerRequest: number
}

export interface DashboardSummary {
  planName: string
  requestsToday: number
  dailyQuota: number
  requestsThisMonth: number
  monthlyQuota: number
  remainingQuota: number
  rateLimit: number
  rateLimitWindowSeconds: number
  successRate: number
  errorRate: number
  avgLatencyMs: number | null
}

export interface UsageRecord {
  id: string
  endpoint: string
  httpMethod: string
  requestedAt: string
  responseStatus: number | null
  responseTimeMs: number | null
  allowed: boolean
  rejectionReason: string
  rateLimitStrategy: string | null
  requestCost: number
}

export interface AnalyticsResponse {
  totalRequests: number
  requestsToday: number
  requestsThisMonth: number
  successfulRequests: number
  failedRequests: number
  rejectedRequests: number
  rateLimitViolations: number
  quotaViolations: number
  avgLatencyMs: number | null
  requestsOverTime: Record<string, unknown>[]
  topApis: Record<string, unknown>[]
  topConsumers: Record<string, unknown>[]
}

export interface BillingResponse {
  planName: string
  monthlyPrice: number
  includedRequests: number
  actualRequests: number
  overageRequests: number
  overagePricePerRequest: number
  overageAmount: number
  estimatedTotal: number
  periodStart: string
  periodEnd: string
  stripeCustomerId: string | null
  stripeSubscriptionId: string | null
}
