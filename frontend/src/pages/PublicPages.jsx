import React, { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { publicApi } from '../api/index.js'
import { AdBanner, Pagination } from '../components/Components.jsx'
import { useAuthCtx } from '../App.jsx'

// ── LandingPage ───────────────────────────────────────────────────────────────
export function LandingPage() {
  const { session } = useAuthCtx()
  const navigate    = useNavigate()
  const [stats, setStats] = useState({ totalUsers: 0, totalCharacters: 0 })

  useEffect(() => {
    publicApi.get('/public/stats').then(setStats).catch(() => {})
  }, [])

  return (
    <div className="page">
      {/* 히어로 섹션 */}
      <div className="text-center" style={{ padding: '48px 0 32px' }}>
        <h1 style={{ fontSize: 36, fontWeight: 800, color: 'var(--gold)', marginBottom: 12 }}>
          ⚔ AION2 퀘스트 체커
        </h1>
        <p style={{ fontSize: 16, color: 'var(--text2)', marginBottom: 32, maxWidth: 480, margin: '0 auto 32px' }}>
          아이온2 일일 · 주간 퀘스트 완료 현황을 한눈에 관리하세요.<br />
          캐릭터별 진행률, 서버 랭킹, 공식 공지까지.
        </p>
        {session ? (
          <button className="btn btn-primary" onClick={() => navigate('/dashboard')}
            style={{ fontSize: 15, padding: '12px 32px' }}>
            대시보드 바로가기 →
          </button>
        ) : (
          <div className="flex gap-8" style={{ justifyContent: 'center' }}>
            <button className="btn btn-primary" onClick={() => navigate('/login')}
              style={{ fontSize: 15, padding: '12px 28px' }}>
              시작하기
            </button>
            <button className="btn btn-ghost" onClick={() => navigate('/ranking')}
              style={{ fontSize: 15, padding: '12px 28px' }}>
              랭킹 보기
            </button>
          </div>
        )}
      </div>

      {/* 통계 */}
      <div className="grid-3" style={{ maxWidth: 480, margin: '0 auto 40px' }}>
        {[
          { label: '등록 유저', value: stats.totalUsers + '명' },
          { label: '등록 캐릭터', value: stats.totalCharacters + '개' },
          { label: '서비스', value: '무료' },
        ].map(s => (
          <div key={s.label} className="card text-center">
            <div style={{ fontSize: 22, fontWeight: 700, color: 'var(--gold)' }}>{s.value}</div>
            <div className="text-muted text-small mt-4">{s.label}</div>
          </div>
        ))}
      </div>

      {/* 기능 소개 */}
      <div className="grid-3" style={{ marginBottom: 40 }}>
        {[
          { icon: '✓', title: '퀘스트 체크', desc: '일일·주간 퀘스트를 캐릭터별로 체크하고 진행률을 확인합니다' },
          { icon: '🏆', title: '서버 랭킹', desc: '등록된 캐릭터 중 전투력·레벨 기준 서버 랭킹을 확인합니다' },
          { icon: '📢', title: '공지 자동 요약', desc: '아이온2 공식 공지를 자동으로 수집해 3줄 요약으로 제공합니다' },
        ].map(f => (
          <div key={f.title} className="card">
            <div style={{ fontSize: 28, marginBottom: 8 }}>{f.icon}</div>
            <div style={{ fontWeight: 600, marginBottom: 6 }}>{f.title}</div>
            <p className="text-muted text-small">{f.desc}</p>
          </div>
        ))}
      </div>

      <AdBanner slot="landing-bottom" />

      <div className="text-center mt-24">
        <Link to="/privacy" className="text-small text-muted">개인정보처리방침</Link>
      </div>
    </div>
  )
}

// ── LoginPage ─────────────────────────────────────────────────────────────────
export function LoginPage() {
  const { login, session } = useAuthCtx()
  const navigate = useNavigate()
  const [form, setForm]   = useState({ username: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => { if (session) navigate('/dashboard') }, [session])

  const handleSubmit = async e => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(form.username, form.password)
      navigate('/dashboard')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page" style={{ maxWidth: 400, paddingTop: 80 }}>
      <div className="card">
        <h2 style={{ marginBottom: 24, textAlign: 'center' }}>로그인</h2>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="label">아이디</label>
            <input className="input" value={form.username}
              onChange={e => setForm(p => ({ ...p, username: e.target.value }))}
              placeholder="아이디 입력" autoFocus />
          </div>
          <div className="form-group">
            <label className="label">비밀번호</label>
            <input className="input" type="password" value={form.password}
              onChange={e => setForm(p => ({ ...p, password: e.target.value }))}
              placeholder="비밀번호 입력" />
          </div>
          {error && <p className="text-danger text-small mb-8">{error}</p>}
          <button className="btn btn-primary w-full" disabled={loading}>
            {loading ? '로그인 중...' : '로그인'}
          </button>
        </form>
        <p className="text-center text-small text-muted mt-16">
          계정은 관리자에게 문의해주세요
        </p>
      </div>
    </div>
  )
}

// ── RankingPage ───────────────────────────────────────────────────────────────
export function RankingPage() {
  const SERVERS = ['카이나토스', '에이온', '아트레이아']
  const [server, setServer]   = useState(SERVERS[0])
  const [ranking, setRanking] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setLoading(true)
    publicApi.get(`/public/ranking?server=${encodeURIComponent(server)}`)
      .then(setRanking).catch(() => setRanking([]))
      .finally(() => setLoading(false))
  }, [server])

  return (
    <div className="page">
      <AdBanner slot="ranking-top" />

      <h1 className="page-title">서버 랭킹</h1>
      <p className="page-sub">AION2 퀘스트 체커에 등록된 캐릭터 기준 랭킹입니다</p>

      <div className="tabs">
        {SERVERS.map(s => (
          <button key={s} className={`tab-btn ${server === s ? 'active' : ''}`}
            onClick={() => setServer(s)}>{s}</button>
        ))}
      </div>

      {loading ? (
        <div className="spinner" />
      ) : ranking.length === 0 ? (
        <div className="empty-state">
          <p>아직 등록된 캐릭터가 없습니다</p>
        </div>
      ) : (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <table className="table">
            <thead>
              <tr>
                <th style={{ width: 50 }}>#</th>
                <th>캐릭터</th>
                <th>직업</th>
                <th>레벨</th>
                <th>전투력</th>
                <th>등급</th>
              </tr>
            </thead>
            <tbody>
              {ranking.map(r => (
                <tr key={r.rank} className={r.rank <= 3 ? 'top3' : ''}>
                  <td style={{ fontWeight: 700 }}>
                    {r.rank === 1 ? '🥇' : r.rank === 2 ? '🥈' : r.rank === 3 ? '🥉' : r.rank}
                  </td>
                  <td>
                    {r.displayName}
                    {r.verified && <span className="verified-badge ml-4">✓ 인증</span>}
                  </td>
                  <td className="text-muted">{r.className || '-'}</td>
                  <td>Lv.{r.level}</td>
                  <td>{r.combatPower?.toLocaleString()}</td>
                  <td>
                    {r.grade && (
                      <span className="badge badge-gold" style={r.gradeColor ? { color: '#' + r.gradeColor } : {}}>
                        {r.grade}
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <AdBanner slot="ranking-bottom" />
    </div>
  )
}

// ── NoticePage ────────────────────────────────────────────────────────────────
export function NoticePage() {
  const CATEGORIES = [
    { value: null,     label: '전체' },
    { value: 'NOTICE', label: '공지' },
    { value: 'UPDATE', label: '업데이트' },
    { value: 'EVENT',  label: '이벤트' },
  ]
  const [category, setCategory] = useState(null)
  const [posts, setPosts]       = useState([])
  const [page, setPage]         = useState(0)
  const [totalPages, setTotal]  = useState(0)
  const [loading, setLoading]   = useState(false)

  useEffect(() => {
    setLoading(true)
    const cat = category ? `&category=${category}` : ''
    publicApi.get(`/public/posts?page=${page}&size=10${cat}`)
      .then(data => { setPosts(data.content); setTotal(data.totalPages) })
      .catch(() => setPosts([]))
      .finally(() => setLoading(false))
  }, [category, page])

  const formatDate = dt => {
    if (!dt) return ''
    return new Date(dt).toLocaleDateString('ko-KR', { year: 'numeric', month: 'short', day: 'numeric' })
  }

  return (
    <div className="page">
      <AdBanner slot="notice-top" />

      <h1 className="page-title">공지 & 업데이트</h1>
      <p className="page-sub">아이온2 공식 공지를 자동으로 수집해 요약 제공합니다</p>

      <div className="tabs">
        {CATEGORIES.map(c => (
          <button key={c.label}
            className={`tab-btn ${category === c.value ? 'active' : ''}`}
            onClick={() => { setCategory(c.value); setPage(0) }}>
            {c.label}
          </button>
        ))}
      </div>

      {loading ? <div className="spinner" /> : (
        <>
          {posts.length === 0 ? (
            <div className="empty-state"><p>등록된 공지가 없습니다</p></div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {posts.map(post => (
                <div key={post.id} className="card">
                  <div className="flex items-center gap-8 mb-8">
                    <span className={`badge badge-${post.category.toLowerCase()}`}>
                      {post.category === 'NOTICE' ? '공지' : post.category === 'UPDATE' ? '업데이트' : '이벤트'}
                    </span>
                    <span className="text-small text-muted">{formatDate(post.createdAt)}</span>
                  </div>
                  <h3 style={{ fontSize: 15, fontWeight: 600, marginBottom: 8 }}>{post.title}</h3>
                  <p style={{ fontSize: 13, color: 'var(--text2)', lineHeight: 1.7 }}>{post.content}</p>
                  {post.sourceUrl && (
                    <a href={post.sourceUrl} target="_blank" rel="noopener noreferrer"
                      className="text-small mt-8" style={{ display: 'inline-block' }}>
                      원문 보기 ↗
                    </a>
                  )}
                </div>
              ))}
            </div>
          )}
          <Pagination current={page} total={totalPages} onChange={setPage} />
        </>
      )}

      <AdBanner slot="notice-bottom" />
    </div>
  )
}

// ── GuidePage ─────────────────────────────────────────────────────────────────
export function GuidePage() {
  return (
    <div className="page" style={{ maxWidth: 720 }}>
      <h1 className="page-title">사용 가이드</h1>
      <p className="page-sub">AION2 퀘스트 체커 사용 방법을 안내합니다</p>

      <AdBanner slot="guide-top" />

      {[
        {
          title: '1. 계정 발급',
          content: '관리자에게 아이디/비밀번호 발급을 요청하세요. 현재는 관리자가 직접 계정을 생성합니다.'
        },
        {
          title: '2. 캐릭터 등록',
          content: '로그인 후 [캐릭터 관리] 페이지에서 서버와 캐릭터명을 입력해 등록하세요. 계정당 최대 16개까지 등록할 수 있습니다.'
        },
        {
          title: '3. 퀘스트 체크',
          content: '[대시보드]에서 캐릭터별 퀘스트 목록을 확인하고 완료한 퀘스트를 클릭해 체크하세요. 일일 퀘스트는 매일, 주간 퀘스트는 매주 월요일에 자동 초기화됩니다.'
        },
        {
          title: '4. 캐릭터 인증',
          content: '캐릭터 소유권을 확인하려면 인증을 진행하세요. [캐릭터 관리]에서 인증 버튼을 클릭하면 인증 코드가 발급됩니다. 아이온2 공식 게시글에 댓글로 코드를 입력하면 인증이 완료됩니다.'
        },
        {
          title: '5. 서버 랭킹',
          content: '[랭킹] 페이지에서 등록된 캐릭터의 서버별 전투력 순위를 확인할 수 있습니다. 로그인 없이도 열람 가능합니다.'
        },
        {
          title: '자주 묻는 질문',
          content: 'Q. 퀘스트가 안 보여요\nA. 캐릭터 레벨에 맞는 퀘스트가 없을 수 있습니다. 관리자에게 퀘스트 템플릿 추가를 요청하세요.\n\nQ. 인증 코드가 만료됐어요\nA. 10분 이내에 인증하지 않으면 만료됩니다. [인증받기]를 다시 클릭해 새 코드를 발급받으세요.'
        },
      ].map(section => (
        <div key={section.title} className="card" style={{ marginBottom: 12 }}>
          <h3 style={{ fontWeight: 600, marginBottom: 10 }}>{section.title}</h3>
          <p style={{ fontSize: 13, color: 'var(--text2)', lineHeight: 1.8, whiteSpace: 'pre-line' }}>
            {section.content}
          </p>
        </div>
      ))}

      <AdBanner slot="guide-bottom" />
    </div>
  )
}

// ── PrivacyPage ───────────────────────────────────────────────────────────────
export function PrivacyPage() {
  return (
    <div className="page" style={{ maxWidth: 720 }}>
      <h1 className="page-title">개인정보처리방침</h1>
      <p className="text-muted text-small mb-16">최종 수정일: 2026년 3월</p>
      <div className="card">
        {[
          ['수집하는 개인정보', '아이디, 게임 캐릭터명(서버명 포함). 비밀번호는 BCrypt 암호화하여 저장하며 원문을 보관하지 않습니다.'],
          ['수집 목적', '퀘스트 체크 서비스 제공, 캐릭터 인증, 랭킹 서비스 제공'],
          ['보유 기간', '회원 탈퇴 시 즉시 삭제. 관계 법령에 따라 보존이 필요한 경우 해당 기간 동안 보관'],
          ['제3자 제공', '개인정보를 외부에 제공하지 않습니다. 단, Google AdSense를 통해 광고가 게재될 수 있으며 이에 따른 쿠키가 설정될 수 있습니다.'],
          ['광고 및 쿠키', '본 사이트는 Google AdSense를 사용합니다. Google은 쿠키를 사용하여 관심 기반 광고를 게재할 수 있습니다. Google 개인정보 처리방침(policies.google.com)을 참고하세요.'],
          ['권리', '사용자는 언제든지 계정 삭제 및 개인정보 삭제를 요청할 수 있습니다.'],
          ['문의', '개인정보 관련 문의는 관리자에게 연락해주세요.'],
        ].map(([title, content]) => (
          <div key={title} style={{ marginBottom: 20 }}>
            <h4 style={{ fontWeight: 600, marginBottom: 6 }}>{title}</h4>
            <p style={{ fontSize: 13, color: 'var(--text2)', lineHeight: 1.8 }}>{content}</p>
          </div>
        ))}
      </div>
    </div>
  )
}
