import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { publicApi } from '../api/index.js'
import { AdBanner, Pagination, ServerSelect } from '../components/Components.jsx'
import { useAuthCtx } from '../App.jsx'

export default function PartyBoardPage() {
  const { session } = useAuthCtx()
  const navigate = useNavigate()

  const [categories, setCategories] = useState([])
  const [posts, setPosts]           = useState([])
  const [totalPages, setTotal]      = useState(0)
  const [loading, setLoading]       = useState(true)

  // 필터 상태
  const [selCat, setSelCat]   = useState(null)
  const [selSub, setSelSub]   = useState(null)
  const [selServer, setServer] = useState('')
  const [page, setPage]        = useState(0)

  // 카테고리 로드
  useEffect(() => {
    publicApi.get('/public/../party/categories').then(setCategories).catch(() => {})
  }, [])

  const load = useCallback(() => {
    setLoading(true)
    const params = new URLSearchParams({ page, size: 20 })
    if (selCat)    params.set('categoryId', selCat)
    if (selSub)    params.set('subcategoryId', selSub)
    if (selServer) params.set('server', selServer)
    publicApi.get('/party/posts?' + params.toString())
      .then(d => { setPosts(d.content); setTotal(d.totalPages) })
      .catch(() => setPosts([]))
      .finally(() => setLoading(false))
  }, [selCat, selSub, selServer, page])

  useEffect(() => { load() }, [load])

  const fmt = dt => {
    if (!dt) return ''
    const d = new Date(dt), now = new Date()
    const diff = (now - d) / 1000
    if (diff < 60) return '방금'
    if (diff < 3600) return Math.floor(diff / 60) + '분 전'
    if (diff < 86400) return Math.floor(diff / 3600) + '시간 전'
    return d.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
  }

  const currentSubs = selCat
    ? (categories.find(c => String(c.id) === String(selCat))?.subcategories || [])
    : []

  return (
    <div className="page">
      <AdBanner slot="party-top" />

      <div className="flex items-center justify-between mb-16">
        <div>
          <h1 className="page-title">파티 모집</h1>
          <p className="page-sub">함께할 파티원을 찾아보세요</p>
        </div>
        {session
          ? <button className="btn btn-primary" onClick={() => navigate('/party/write')}>모집 글 작성</button>
          : <button className="btn btn-ghost" onClick={() => navigate('/login')}>로그인 후 작성</button>
        }
      </div>

      {/* 필터 영역 */}
      <div className="card mb-16">
        <div className="flex gap-8" style={{ flexWrap: 'wrap' }}>
          {/* 1차 카테고리 */}
          <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', flex: 1 }}>
            <button
              className={`btn btn-sm ${!selCat ? 'btn-primary' : 'btn-ghost'}`}
              onClick={() => { setSelCat(null); setSelSub(null); setPage(0) }}>
              전체
            </button>
            {categories.map(c => (
              <button key={c.id}
                className={`btn btn-sm ${String(selCat) === String(c.id) ? 'btn-primary' : 'btn-ghost'}`}
                onClick={() => { setSelCat(c.id); setSelSub(null); setPage(0) }}>
                {c.icon} {c.name}
              </button>
            ))}
          </div>

          {/* 서버 필터 */}
          <select className="select" style={{ width: 140 }} value={selServer}
            onChange={e => { setServer(e.target.value); setPage(0) }}>
            <option value="">모든 서버</option>
            {['카이나토스', '에이온', '아트레이아'].map(s => <option key={s} value={s}>{s}</option>)}
          </select>
        </div>

        {/* 2차 카테고리 */}
        {currentSubs.length > 0 && (
          <div className="flex gap-6 mt-8" style={{ flexWrap: 'wrap' }}>
            <button
              className={`btn btn-xs ${!selSub ? 'btn-primary' : 'btn-ghost'}`}
              onClick={() => { setSelSub(null); setPage(0) }}>
              전체
            </button>
            {currentSubs.map(s => (
              <button key={s.id}
                className={`btn btn-xs ${String(selSub) === String(s.id) ? 'btn-primary' : 'btn-ghost'}`}
                onClick={() => { setSelSub(s.id); setPage(0) }}>
                {s.name}{s.minLevel ? ` (Lv.${s.minLevel}+)` : ''}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* 글 목록 */}
      {loading ? <div className="spinner" /> : posts.length === 0 ? (
        <div className="empty-state">
          <p>모집 중인 파티가 없습니다</p>
          {session && <button className="btn btn-primary mt-16" onClick={() => navigate('/party/write')}>첫 모집 글 작성하기</button>}
        </div>
      ) : (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          {posts.map(post => (
            <div key={post.id} className="party-post-row" onClick={() => navigate(`/party/${post.id}`)}>
              <div className="flex items-center gap-8">
                <span className="badge badge-blue" style={{ fontSize: 10 }}>{post.categoryName}</span>
                <span className="badge badge-gray" style={{ fontSize: 10 }}>{post.subcategoryName}</span>
                {post.server && <span className="text-small text-muted">{post.server}</span>}
                {post.minLevel && <span className="text-small text-muted">Lv.{post.minLevel}+</span>}
                <span className={`party-members ${post.currentMembers >= post.maxMembers ? 'full' : 'open'}`}>
                  {post.currentMembers}/{post.maxMembers}명
                </span>
                {post.status === 'CLOSED' && <span className="badge badge-gray" style={{ fontSize: 10 }}>모집완료</span>}
              </div>
              <div className="party-post-title">{post.title}</div>
              <div className="party-post-meta">
                <span className={`provider-dot ${post.authorProvider}`} />
                <span className="text-small text-muted">{post.authorName}</span>
                <span className="text-small text-muted">·</span>
                <span className="text-small text-muted">조회 {post.views}</span>
                <span className="text-small text-muted">·</span>
                <span className="text-small text-muted">댓글 {post.commentCount}</span>
                <span className="text-small text-muted" style={{ marginLeft: 'auto' }}>{fmt(post.createdAt)}</span>
              </div>
            </div>
          ))}
        </div>
      )}

      <Pagination current={page} total={totalPages} onChange={setPage} />
      <AdBanner slot="party-bottom" />
    </div>
  )
}
