// hooks/useAuth.js
import { useState, useCallback } from 'react'
import { createApi } from '../api/index.js'

const TOKEN_KEY   = 'aion2_token'
const SESSION_KEY = 'aion2_session'

export function useAuth() {
  const [session, setSession] = useState(() => {
    try { return JSON.parse(localStorage.getItem(SESSION_KEY)) || null }
    catch { return null }
  })

  const token = localStorage.getItem(TOKEN_KEY)
  const api   = createApi(token)

  const login = useCallback(async (username, password) => {
    const data = await createApi(null).post('/auth/login', { username, password })
    localStorage.setItem(TOKEN_KEY,   data.token)
    localStorage.setItem(SESSION_KEY, JSON.stringify({
      id: data.id, username: data.username, role: data.role
    }))
    setSession({ id: data.id, username: data.username, role: data.role })
    return data
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(SESSION_KEY)
    setSession(null)
  }, [])

  return { session, api, login, logout, isAdmin: session?.role === 'ADMIN' }
}
