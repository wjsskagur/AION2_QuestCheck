import React, { useState, useEffect } from 'react'
import { useNavigate, useSearchParams, Link } from 'react-router-dom'
import { publicApi, createApi } from '../api/index.js'
import { AdBanner } from '../components/Components.jsx'
import { useAuthCtx } from '../App.jsx'

// ── LandingPage ───────────────────────────────────────────────────────────────
export function LandingPage() {
  const { session } = useAuthCtx()
  const navigate = useNavigate()
  const [stats, setStats] = useState({ totalUsers: 0, totalCharacters: 0 })
  useEffect(() => { publicApi.get('/public/stats').then(setStats).catch(() => {}) }, [])

  return (
    <div className="page">
      <div className="text-center" style={{ padding: '48px 0 32px' }}>
        <h1 style={{ fontSize: 36, fontWeight: 800, color: 'var(--gold)', marginBottom: 12 }}>⚔ AION2 퀘스트 체커</h1>
        <p style={{ fontSize: 16, color: 'var(--text2)', marginBottom: 32, maxWidth: 480, margin: '0 auto 32px' }}>
          일일·주간 퀘스트 관리, 파티 모집, 서버 랭킹까지<br />아이온2 플레이어를 위한 종합 도우미
        </p>
        <div className="flex gap-8" style={{ justifyContent: 'center' }}>
          {session
            ? <button className="btn btn-primary" style={{ fontSize: 15, padding: '12px 28px' }} onClick={() => navigate('/dashboard')}>대시보드 →</button>
            : <button className="btn btn-primary" style={{ fontSize: 15, padding: '12px 28px' }} onClick={() => navigate('/login')}>시작하기</button>
          }
          <button className="btn btn-ghost" style={{ fontSize: 15, padding: '12px 28px' }} onClick={() => navigate('/party')}>파티 모집</button>
        </div>
      </div>

      <div className="grid-3" style={{ maxWidth: 600, margin: '0 auto 40px' }}>
        {[{ label: '등록 유저', value: stats.totalUsers + '명' }, { label: '등록 캐릭터', value: stats.totalCharacters + '개' }, { label: '서비스', value: '무료' }].map(s => (
          <div key={s.label} className="card text-center">
            <div style={{ fontSize: 22, fontWeight: 700, color: 'var(--gold)' }}>{s.value}</div>
            <div className="text-muted text-small mt-4">{s.label}</div>
          </div>
        ))}
      </div>

      <div className="grid-3" style={{ marginBottom: 40 }}>
        {[
          { icon: '✓', title: '퀘스트 체커', desc: '일일·주간 퀘스트를 캐릭터별로 체크. 초기화 시간 자동 처리.' },
          { icon: '👥', title: '파티 모집', desc: '던전·레이드·PvP 파티원 모집. 1차·2차 카테고리로 빠르게 찾기.' },
          { icon: '🏆', title: '서버 랭킹', desc: '전투력·레벨 기준 서버 랭킹. 로그인 없이 누구나 확인.' },
        ].map(f => (
          <div key={f.title} className="card">
            <div style={{ fontSize: 28, marginBottom: 8 }}>{f.icon}</div>
            <div style={{ fontWeight: 600, marginBottom: 6 }}>{f.title}</div>
            <p className="text-muted text-small">{f.desc}</p>
          </div>
        ))}
      </div>

      <AdBanner slot="landing-bottom" />
      <div className="text-center mt-24"><Link to="/privacy" className="text-small text-muted">개인정보처리방침</Link></div>
    </div>
  )
}

// ── LoginPage ─────────────────────────────────────────────────────────────────
export function LoginPage() {
  const { login, session } = useAuthCtx()
  const navigate = useNavigate()
  const [mode, setMode]     = useState('login')
  const [form, setForm]     = useState({ username: '', password: '', nickname: '' })
  const [error, setError]   = useState('')
  const [loading, setLoading] = useState(false)
  const API_BASE = import.meta.env.VITE_API_URL || '/api'

  useEffect(() => { if (session) navigate('/dashboard') }, [session])

  const handleSubmit = async e => {
    e.preventDefault(); setError(''); setLoading(true)
    try {
      if (mode === 'login') {
        await login(form.username, form.password)
      } else {
        const d = await createApi(null).post('/auth/signup', { username: form.username, password: form.password, nickname: form.nickname || null })
        localStorage.setItem('aion2_token', d.token)
        localStorage.setItem('aion2_session', JSON.stringify({ id: d.id, username: d.username, nickname: d.nickname, role: d.role, authProvider: d.authProvider }))
        window.location.href = '/dashboard'; return
      }
      navigate('/dashboard')
    } catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }

  return (
    <div className="page" style={{ maxWidth: 400, paddingTop: 60 }}>
      <div className="card">
        <div className="tabs">
          {[['login', '로그인'], ['signup', '회원가입']].map(([m, l]) => (
            <button key={m} className={`tab-btn ${mode === m ? 'active' : ''}`} style={{ flex: 1, fontSize: 14 }} onClick={() => { setMode(m); setError('') }}>{l}</button>
          ))}
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="label">아이디</label>
            <input className="input" autoFocus placeholder="아이디 입력" value={form.username} onChange={e => setForm(p => ({ ...p, username: e.target.value }))} />
          </div>
          <div className="form-group">
            <label className="label">비밀번호</label>
            <input className="input" type="password" placeholder={mode === 'signup' ? '8자 이상' : '비밀번호 입력'} value={form.password} onChange={e => setForm(p => ({ ...p, password: e.target.value }))} />
          </div>
          {mode === 'signup' && (
            <div className="form-group">
              <label className="label">닉네임 (선택)</label>
              <input className="input" placeholder="표시 이름" value={form.nickname} onChange={e => setForm(p => ({ ...p, nickname: e.target.value }))} />
            </div>
          )}
          {error && <p className="text-danger text-small mb-8">{error}</p>}
          <button className="btn btn-primary w-full" disabled={loading}>{loading ? '처리 중...' : mode === 'login' ? '로그인' : '가입하기'}</button>
        </form>

        <div className="flex items-center gap-8" style={{ margin: '20px 0' }}>
          <div style={{ flex: 1, height: 1, background: 'var(--border)' }} />
          <span className="text-muted text-small">또는 SNS 로그인</span>
          <div style={{ flex: 1, height: 1, background: 'var(--border)' }} />
        </div>

        {[
          { provider: 'kakao',  label: '카카오로 계속하기', bg: '#FEE500', color: '#191919' },
          { provider: 'naver',  label: '네이버로 계속하기', bg: '#03C75A', color: '#fff' },
          { provider: 'google', label: '구글로 계속하기',   bg: 'var(--bg3)', color: 'var(--text1)' },
        ].map(({ provider, label, bg, color }) => (
          <button key={provider} className="btn w-full" style={{ background: bg, color, border: '1px solid var(--border)', justifyContent: 'center', marginBottom: 8 }}
            onClick={() => window.location.href = `${API_BASE}/oauth2/authorization/${provider}`}>
            {label}
          </button>
        ))}
      </div>
    </div>
  )
}

// ── OAuth2Callback ────────────────────────────────────────────────────────────
export function OAuth2Callback() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [error, setError] = useState('')

  useEffect(() => {
    const token    = searchParams.get('token')
    const provider = searchParams.get('provider')
    if (!token) { setError('인증 토큰을 받지 못했습니다.'); return }
    createApi(token).get('/auth/me')
      .then(me => {
        localStorage.setItem('aion2_token', token)
        localStorage.setItem('aion2_session', JSON.stringify({ id: me.id, username: me.username, nickname: me.nickname, role: me.role, authProvider: provider?.toUpperCase() || 'UNKNOWN', profileImage: me.profileImage }))
        navigate('/dashboard', { replace: true })
      })
      .catch(() => setError('로그인 처리 중 오류가 발생했습니다.'))
  }, [])

  if (error) return (
    <div className="page text-center" style={{ paddingTop: 80 }}>
      <p className="text-danger">{error}</p>
      <button className="btn btn-ghost mt-16" onClick={() => navigate('/login')}>다시 로그인</button>
    </div>
  )
  return <div className="page text-center" style={{ paddingTop: 80 }}><div className="spinner" /><p className="text-muted mt-8">로그인 처리 중...</p></div>
}

export default LandingPage
