import axios from 'axios'

// Local Docker: nginx proxies "/api" to the backend. Split hosting (Vercel):
// set VITE_API_BASE_URL to the backend's absolute origin at build time.
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('gw_token')
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('gw_token')
      localStorage.removeItem('gw_user')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)

export interface ApiErrorBody {
  timestamp: string
  status: number
  error: string
  message: string
  retryAfter?: number
}
