const BASE = import.meta.env.VITE_API_URL || '/api'
export function createApi(token) {
  const h = { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }
  const req = async (method, path, body) => {
    const res = await fetch(`${BASE}${path}`, { method, headers: h, body: body ? JSON.stringify(body) : undefined })
    if (res.status === 204) return null
    const data = await res.json()
    if (!res.ok) throw new Error(data.message || `HTTP ${res.status}`)
    return data
  }
  return { get: p => req('GET', p), post: (p, b) => req('POST', p, b), put: (p, b) => req('PUT', p, b), patch: (p, b) => req('PATCH', p, b), delete: p => req('DELETE', p) }
}
export const publicApi = createApi(null)
