import React, { createContext, useContext } from 'react'
import { BrowserRouter, Routes, Route, Navigate, Outlet, NavLink } from 'react-router-dom'
import { useAuth } from './hooks/useAuth.js'

// ── Pages ──
import LandingPage    from './pages/LandingPage.jsx'
import LoginPage      from './pages/LoginPage.jsx'
import Dashboard      from './pages/Dashboard.jsx'
import CharactersPage from './pages/CharactersPage.jsx'
import CharDetailPage from './pages/CharDetailPage.jsx'
import RankingPage    from './pages/RankingPage.jsx'
import NoticePage     from './pages/NoticePage.jsx'
import GuidePage      from './pages/GuidePage.jsx'
import PrivacyPage    from './pages/PrivacyPage.jsx'
import AdminPage      from './pages/AdminPage.jsx'

// ── Auth Context ──
export const AuthContext = createContext(null)
export const useAuthCtx = () => useContext(AuthContext)

function PrivateRoute() {
  const token = localStorage.getItem('aion2_token')
  return token ? <Outlet /> : <Navigate to="/login" replace />
}

function AdminRoute() {
  const session = JSON.parse(localStorage.getItem('aion2_session') || '{}')
  return session?.role === 'ADMIN' ? <Outlet /> : <Navigate to="/dashboard" replace />
}

function Navbar() {
  const { session, logout } = useAuthCtx()
  return (
    <nav className="navbar">
      <NavLink to="/" className="navbar-brand">⚔ AION2 체커</NavLink>
      <div className="navbar-links">
        <NavLink to="/ranking">랭킹</NavLink>
        <NavLink to="/notice">공지</NavLink>
        <NavLink to="/guide">가이드</NavLink>
        {session && <NavLink to="/dashboard">대시보드</NavLink>}
        {session?.role === 'ADMIN' && <NavLink to="/admin">관리자</NavLink>}
      </div>
      <div className="navbar-right">
        {session ? (
          <>
            <span className="text-muted text-small">{session.username}</span>
            <button className="btn btn-ghost btn-sm" onClick={logout}>로그아웃</button>
          </>
        ) : (
          <NavLink to="/login" className="btn btn-primary btn-sm">로그인</NavLink>
        )}
      </div>
    </nav>
  )
}

export default function App() {
  const auth = useAuth()
  return (
    <AuthContext.Provider value={auth}>
      <BrowserRouter>
        <Navbar />
        <Routes>
          {/* 공개 라우트 */}
          <Route path="/"        element={<LandingPage />} />
          <Route path="/login"   element={<LoginPage />} />
          <Route path="/ranking" element={<RankingPage />} />
          <Route path="/notice"  element={<NoticePage />} />
          <Route path="/guide"   element={<GuidePage />} />
          <Route path="/privacy" element={<PrivacyPage />} />

          {/* 보호 라우트 (JWT 필요) */}
          <Route element={<PrivateRoute />}>
            <Route path="/dashboard"       element={<Dashboard />} />
            <Route path="/characters"      element={<CharactersPage />} />
            <Route path="/characters/:id"  element={<CharDetailPage />} />
          </Route>

          {/* 관리자 전용 */}
          <Route element={<AdminRoute />}>
            <Route path="/admin" element={<AdminPage />} />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthContext.Provider>
  )
}
