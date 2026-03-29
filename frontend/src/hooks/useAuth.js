import { useState, useCallback } from 'react'
import { createApi } from '../api/index.js'
const TK = 'aion2_token', SK = 'aion2_session'
export function useAuth() {
  const [session, setSession] = useState(() => { try { return JSON.parse(localStorage.getItem(SK)) || null } catch { return null } })
  const token = localStorage.getItem(TK)
  const api   = createApi(token)
  const login = useCallback(async (username, password) => {
    const d = await createApi(null).post('/auth/login', { username, password })
    localStorage.setItem(TK, d.token)
    const s = { id: d.id, username: d.username, nickname: d.nickname, role: d.role, authProvider: d.authProvider, profileImage: d.profileImage }
    localStorage.setItem(SK, JSON.stringify(s)); setSession(s); return d
  }, [])
  const logout = useCallback(() => { localStorage.removeItem(TK); localStorage.removeItem(SK); setSession(null) }, [])
  return { session, api, login, logout, isAdmin: session?.role === 'ADMIN' }
}
