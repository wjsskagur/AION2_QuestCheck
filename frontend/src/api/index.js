// api/index.js
const BASE = import.meta.env.VITE_API_URL || '/api'

export function createApi(token) {
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }
  const req = async (method, path, body) => {
    const res = await fetch(`${BASE}${path}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    })
    if (res.status === 204) return null
    const data = await res.json()
    if (!res.ok) throw new Error(data.message || `HTTP ${res.status}`)
    return data
  }
  return {
    get:    path       => req('GET',    path),
    post:   (path, b)  => req('POST',   path, b),
    put:    (path, b)  => req('PUT',    path, b),
    delete: path       => req('DELETE', path),
  }
}

export const publicApi = createApi(null)
