const serverOrigin = String(import.meta.env.VITE_SERVER_ORIGIN || '').replace(/\/+$/, '')

export const API_BASE_URL = serverOrigin ? `${serverOrigin}/api` : '/api'

export function apiUrl(path) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${API_BASE_URL}${normalizedPath}`
}

export function resolveServerUrl(path) {
  if (!path || /^(https?:|data:|blob:)/i.test(path)) return path || ''
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${serverOrigin}${normalizedPath}`
}

export function createWebSocketUrl(path, params = {}) {
  const baseOrigin = serverOrigin || window.location.origin
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const url = new URL(normalizedPath, `${baseOrigin}/`)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, value)
    }
  })
  return url.toString()
}
