import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { publicApi } from '../api/index.js'
import { AdBanner, Pagination } from '../components/Components.jsx'

const SERVERS = ['카이나토스', '에이온', '아트레이아']

// ── RankingPage ───────────────────────────────────────────────────────────────
export function RankingPage() {
  const [server, setServer]   = useState(SERVERS[0])
  const [ranking, setRanking] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setLoading(true)
    publicApi.get(`/public/ranking?server=${encodeURIComponent(server)}`).then(setRanking).catch(() => setRanking([])).finally(() => setLoading(false))
  }, [server])

  return (
    <div className="page">
      <AdBanner slot="ranking-top" />
      <h1 className="page-title">서버 랭킹</h1>
      <p className="page-sub">AION2 퀘스트 체커에 등록된 캐릭터 기준 랭킹입니다</p>
      <div className="tabs">
        {SERVERS.map(s => <button key={s} className={`tab-btn ${server === s ? 'active' : ''}`} onClick={() => setServer(s)}>{s}</button>)}
      </div>
      {loading ? <div className="spinner" /> : ranking.length === 0 ? (
        <div className="empty-state"><p>아직 등록된 캐릭터가 없습니다</p></div>
      ) : (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <table className="table">
            <thead><tr><th style={{ width: 50 }}>#</th><th>캐릭터</th><th>직업</th><th>레벨</th><th>전투력</th><th>등급</th></tr></thead>
            <tbody>
              {ranking.map(r => (
                <tr key={r.rank} className={r.rank <= 3 ? 'top3' : ''}>
                  <td style={{ fontWeight: 700 }}>{r.rank === 1 ? '🥇' : r.rank === 2 ? '🥈' : r.rank === 3 ? '🥉' : r.rank}</td>
                  <td>{r.displayName}{r.verified && <span className="verified-badge" style={{ marginLeft: 6 }}>✓</span>}</td>
                  <td className="text-muted">{r.className || '-'}</td>
                  <td>Lv.{r.level}</td>
                  <td>{r.combatPower?.toLocaleString()}</td>
                  <td>{r.grade && <span className="badge badge-gold">{r.grade}</span>}</td>
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
  const CATS = [{ value: null, label: '전체' }, { value: 'NOTICE', label: '공지' }, { value: 'UPDATE', label: '업데이트' }, { value: 'EVENT', label: '이벤트' }]
  const [category, setCategory] = useState(null)
  const [posts, setPosts]       = useState([])
  const [page, setPage]         = useState(0)
  const [totalPages, setTotal]  = useState(0)
  const [loading, setLoading]   = useState(false)

  useEffect(() => {
    setLoading(true)
    publicApi.get(`/public/posts?page=${page}&size=10${category ? '&category=' + category : ''}`).then(d => { setPosts(d.content); setTotal(d.totalPages) }).catch(() => setPosts([])).finally(() => setLoading(false))
  }, [category, page])

  const fmt = dt => dt ? new Date(dt).toLocaleDateString('ko-KR', { year: 'numeric', month: 'short', day: 'numeric' }) : ''

  return (
    <div className="page">
      <AdBanner slot="notice-top" />
      <h1 className="page-title">공지 & 업데이트</h1>
      <p className="page-sub">아이온2 공식 공지를 자동으로 수집해 요약 제공합니다</p>
      <div className="tabs">
        {CATS.map(c => <button key={c.label} className={`tab-btn ${category === c.value ? 'active' : ''}`} onClick={() => { setCategory(c.value); setPage(0) }}>{c.label}</button>)}
      </div>
      {loading ? <div className="spinner" /> : posts.length === 0 ? <div className="empty-state"><p>등록된 공지가 없습니다</p></div> : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {posts.map(post => (
            <div key={post.id} className="card">
              <div className="flex items-center gap-8 mb-8">
                <span className={`badge badge-${post.category.toLowerCase()}`}>{post.category === 'NOTICE' ? '공지' : post.category === 'UPDATE' ? '업데이트' : '이벤트'}</span>
                <span className="text-small text-muted">{fmt(post.createdAt)}</span>
              </div>
              <h3 style={{ fontSize: 15, fontWeight: 600, marginBottom: 8 }}>{post.title}</h3>
              <p style={{ fontSize: 13, color: 'var(--text2)', lineHeight: 1.7 }}>{post.content}</p>
              {post.sourceUrl && <a href={post.sourceUrl} target="_blank" rel="noopener noreferrer" className="text-small mt-8" style={{ display: 'inline-block' }}>원문 보기 ↗</a>}
            </div>
          ))}
        </div>
      )}
      <Pagination current={page} total={totalPages} onChange={setPage} />
      <AdBanner slot="notice-bottom" />
    </div>
  )
}

// ── GuidePage ─────────────────────────────────────────────────────────────────
export function GuidePage() {
  return (
    <div className="page" style={{ maxWidth: 720 }}>
      <h1 className="page-title">사용 가이드</h1>
      <p className="page-sub">AION2 퀘스트 체커 사용 방법</p>
      <AdBanner slot="guide-top" />
      {[
        { title: '1. 회원가입 / 로그인', content: '직접 아이디를 만들거나 카카오·네이버·구글 소셜 로그인을 이용할 수 있습니다.' },
        { title: '2. 캐릭터 등록', content: '[캐릭터 관리]에서 서버와 캐릭터명을 입력해 등록하세요. 계정당 최대 16개까지 등록 가능합니다.' },
        { title: '3. 퀘스트 체크', content: '[대시보드]에서 완료한 퀘스트를 클릭하세요. 일일 퀘스트는 매일, 주간 퀘스트는 매주 수요일 오전 5시에 자동 초기화됩니다. 퀘스트마다 초기화 시간이 다르게 설정될 수 있습니다.' },
        { title: '4. 파티 모집', content: '[파티모집] 메뉴에서 던전·레이드·PvP 등 원하는 파티를 찾거나 모집 글을 올릴 수 있습니다. 로그인 없이도 목록 조회가 가능합니다.' },
        { title: '5. 캐릭터 인증', content: '[캐릭터 관리]에서 인증받기 버튼 클릭 → 발급된 코드를 아이온2 공식 게시글에 댓글로 작성하면 인증이 완료됩니다.' },
      ].map(s => (
        <div key={s.title} className="card" style={{ marginBottom: 12 }}>
          <h3 style={{ fontWeight: 600, marginBottom: 10 }}>{s.title}</h3>
          <p style={{ fontSize: 13, color: 'var(--text2)', lineHeight: 1.8 }}>{s.content}</p>
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
          ['수집하는 개인정보', '아이디, 게임 캐릭터명(서버명 포함). 비밀번호는 BCrypt 암호화 저장. SNS 로그인 시 해당 플랫폼에서 제공하는 닉네임·이메일·프로필 이미지.'],
          ['수집 목적', '퀘스트 체크 서비스 제공, 파티 모집 게시판 운영, 캐릭터 인증, 랭킹 서비스 제공'],
          ['보유 기간', '회원 탈퇴 시 즉시 삭제. 관계 법령에 따라 보존이 필요한 경우 해당 기간 동안 보관'],
          ['제3자 제공', '개인정보를 외부에 제공하지 않습니다. Google AdSense를 통해 광고가 게재될 수 있습니다.'],
          ['광고 및 쿠키', '본 사이트는 Google AdSense를 사용합니다. Google은 쿠키를 사용하여 관심 기반 광고를 게재할 수 있습니다.'],
          ['문의', '개인정보 관련 문의는 관리자에게 연락해주세요.'],
        ].map(([t, c]) => (
          <div key={t} style={{ marginBottom: 20 }}>
            <h4 style={{ fontWeight: 600, marginBottom: 6 }}>{t}</h4>
            <p style={{ fontSize: 13, color: 'var(--text2)', lineHeight: 1.8 }}>{c}</p>
          </div>
        ))}
      </div>
    </div>
  )
}

export default RankingPage
