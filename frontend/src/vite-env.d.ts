/// <reference types="vite/client" />

interface ImportMetaEnv {
  /**
   * Absolute backend origin (e.g. https://your-backend.onrender.com) used when the
   * frontend is hosted separately from the backend (Vercel). Leave unset for the
   * local Docker setup, where nginx proxies /api to the backend.
   */
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
