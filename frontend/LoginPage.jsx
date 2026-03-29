import React, { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuthCtx } from '../App.jsx'
import { createApi } from '../api/index.js'

const API_BASE = import.meta.env.VITE_API_URL || '/api'

// ── SNS 로그인 버튼 컴포넌트 ─────────────────────────────────────────────────
function SnsButton({ provider, label, color, textColor = '#fff' }) {
  const startOAuth = () => {
    // Spring Security OAuth2 시작 URL로 이동 (백엔드가 SNS 인증 페이지로 리다이렉트)
    window.location.href = `${API_BASE}/oauth2/authorization/${provider}`
  }
  return (
    <button
      onClick={startOAuth}
      className="btn w-full"
      style={{
        background: color,
        color: textColor,
        border: 'none',
        justifyContent: 'center',
        marginBottom: 8,
        fontSize: 14,
      }}>
      {label}
    </button>
  )
}

// ── 로그인 / 회원가입 페이지 ──────────────────────────────────────────────────
export function LoginPage() {
  const { login, session } = useAuthCtx()
  const navigate = useNavigate()
  const [mode, setMode] = useState('login') // 'login' | 'signup'
  const [form, setForm] = useState({ username: '', password: '', nickname: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => { if (session) navigate('/dashboard') }, [session])

  const handleSubmit = async e => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      if (mode === 'login') {
        await login(form.username, form.password)
      } else {
        // 회원가입 후 자동 로그인
        const data = await createApi(null).post('/auth/signup', {
          username: form.username,
          password: form.password,
          nickname: form.nickname || null,
        })
        localStorage.setItem('aion2_token', data.token)
        localStorage.setItem('aion2_session', JSON.stringify({
          id: data.id, username: data.username,
          nickname: data.nickname, role: data.role,
          authProvider: data.authProvider,
        }))
        window.location.href = '/dashboard'
        return
      }
      navigate('/dashboard')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page" style={{ maxWidth: 400, paddingTop: 60 }}>
      <div className="card">
        {/* 탭: 로그인 / 회원가입 */}
        <div className="flex" style={{ marginBottom: 24, borderBottom: '1px solid var(--border)' }}>
          {['login', 'signup'].map(m => (
            <button key={m}
              className="tab-btn"
              style={{ flex: 1, fontSize: 14 }}
              onClick={() => { setMode(m); setError('') }}
              data-active={mode === m ? 'true' : undefined}>
              {m === 'login' ? '로그인' : '회원가입'}
            </button>
          ))}
        </div>

        {/* 폼 */}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="label">아이디</label>
            <input className="input" autoFocus
              value={form.username} placeholder="영문+숫자 4~20자"
              onChange={e => setForm(p => ({ ...p, username: e.target.value }))} />
          </div>
          <div className="form-group">
            <label className="label">비밀번호</label>
            <input className="input" type="password"
              value={form.password} placeholder={mode === 'signup' ? '8자 이상' : '비밀번호 입력'}
              onChange={e => setForm(p => ({ ...p, password: e.target.value }))} />
          </div>
          {mode === 'signup' && (
            <div className="form-group">
              <label className="label">닉네임 (선택)</label>
              <input className="input"
                value={form.nickname} placeholder="표시 이름 (비워두면 아이디 사용)"
                onChange={e => setForm(p => ({ ...p, nickname: e.target.value }))} />
            </div>
          )}
          {error && <p className="text-danger text-small mb-8">{error}</p>}
          <button className="btn btn-primary w-full" disabled={loading}>
            {loading ? '처리 중...' : mode === 'login' ? '로그인' : '가입하기'}
          </button>
        </form>

        {/* 구분선 */}
        <div className="flex items-center gap-8" style={{ margin: '20px 0' }}>
          <div style={{ flex: 1, height: 1, background: 'var(--border)' }} />
          <span className="text-muted text-small">또는</span>
          <div style={{ flex: 1, height: 1, background: 'var(--border)' }} />
        </div>

        {/* SNS 로그인 버튼 */}
        <SnsButton
          provider="kakao"
          label="카카오로 계속하기"
          color="#FEE500"
          textColor="#191919"
        />
        <SnsButton
          provider="naver"
          label="네이버로 계속하기"
          color="#03C75A"
          textColor="#fff"
        />
        <SnsButton
          provider="google"
          label="구글로 계속하기"
          color="#fff"
          textColor="#333"
        />

        <p className="text-center text-small text-muted mt-16">
          {mode === 'login'
            ? <span onClick={() => setMode('signup')} style={{ cursor: 'pointer', color: 'var(--blue2)' }}>계정이 없으신가요? 회원가입</span>
            : <span onClick={() => setMode('login')} style={{ cursor: 'pointer', color: 'var(--blue2)' }}>이미 계정이 있으신가요? 로그인</span>
          }
        </p>
      </div>
    </div>
  )
}

// ── OAuth2 콜백 페이지 ────────────────────────────────────────────────────────
// 경로: /oauth2/callback
// SNS 인증 성공 후 백엔드가 이 페이지로 ?token=JWT&provider=kakao 를 붙여 리다이렉트
export function OAuth2CallbackPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [error, setError] = useState('')

  useEffect(() => {
    const token    = searchParams.get('token')
    const provider = searchParams.get('provider')

    if (!token) {
      setError('인증 토큰을 받지 못했습니다. 다시 시도해주세요.')
      return
    }

    // 토큰으로 사용자 정보 조회 후 세션 저장
    createApi(token).get('/auth/me')
      .then(me => {
        localStorage.setItem('aion2_token', token)
        localStorage.setItem('aion2_session', JSON.stringify({
          id: me.id, username: me.username,
          nickname: me.nickname, role: me.role,
          authProvider: provider?.toUpperCase() || 'UNKNOWN',
          profileImage: me.profileImage,
        }))
        navigate('/dashboard', { replace: true })
      })
      .catch(() => {
        setError('로그인 처리 중 오류가 발생했습니다.')
      })
  }, [])

  if (error) {
    return (
      <div className="page text-center" style={{ paddingTop: 80 }}>
        <p className="text-danger">{error}</p>
        <button className="btn btn-ghost mt-16" onClick={() => navigate('/login')}>
          다시 로그인
        </button>
      </div>
    )
  }

  return (
    <div className="page text-center" style={{ paddingTop: 80 }}>
      <div className="spinner" />
      <p className="text-muted mt-8">로그인 처리 중...</p>
    </div>
  )
}
