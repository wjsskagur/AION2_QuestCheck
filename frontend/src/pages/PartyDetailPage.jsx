import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { publicApi } from '../api/index.js'
import { ProviderBadge } from '../components/Components.jsx'
import { useAuthCtx } from '../App.jsx'

export default function PartyDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { session, api } = useAuthCtx()

  const [post, setPost]         = useState(null)
  const [loading, setLoading]   = useState(true)
  const [comment, setComment]   = useState('')
  const [submitting, setSub]    = useState(false)

  const load = useCallback(() => {
    publicApi.get(`/party/posts/${id}`)
      .then(setPost).catch(() => navigate('/party', { replace: true }))
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => { load() }, [load])

  const fmt = dt => dt ? new Date(dt).toLocaleString('ko-KR', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''

  const handleComment = async e => {
    e.preventDefault()
    if (!comment.trim()) return
    setSub(true)
    try { await api.post(`/party/posts/${id}/comments`, { content: comment }); setComment(''); load() }
    catch (err) { alert(err.message) }
    finally { setSub(false) }
  }

  const handleDelete = async () => {
    if (!confirm('게시글을 삭제하시겠습니까?')) return
    await api.delete(`/party/posts/${id}`); navigate('/party')
  }

  const handleClose = async () => {
    if (!confirm('모집을 마감하시겠습니까?')) return
    await api.patch(`/party/posts/${id}/close`); load()
  }

  const handleDelComment = async (cid) => {
    if (!confirm('댓글을 삭제하시겠습니까?')) return
    await api.delete(`/party/comments/${cid}`); load()
  }

  const isAuthor  = session && post && String(session.id) === String(post.accountId)
  const isAdmin   = session?.role === 'ADMIN'

  if (loading) return <div className="page"><div className="spinner" /></div>
  if (!post)   return null

  return (
    <div className="page" style={{ maxWidth: 760 }}>
      <button className="btn btn-ghost btn-sm mb-16" onClick={() => navigate('/party')}>← 파티 모집 목록</button>

      {/* 글 헤더 */}
      <div className="card mb-12">
        {/* 카테고리 배지 */}
        <div className="flex items-center gap-8 mb-12">
          <span className="badge badge-blue">{post.categoryName}</span>
          <span className="badge badge-gray">{post.subcategoryName}</span>
          {post.server && <span className="badge badge-gray">{post.server}</span>}
          {post.minLevel && <span className="badge badge-gray">Lv.{post.minLevel}+</span>}
          <span className={`badge ${post.status === 'OPEN' ? 'badge-open' : 'badge-closed'}`}>
            {post.status === 'OPEN' ? '모집중' : '모집완료'}
          </span>
        </div>

        <h1 style={{ fontSize: 20, fontWeight: 700, marginBottom: 12 }}>{post.title}</h1>

        {/* 메타 정보 */}
        <div className="flex items-center gap-8 mb-16" style={{ flexWrap: 'wrap' }}>
          <div className="flex items-center gap-6">
            {post.authorProfileImage
              ? <img src={post.authorProfileImage} alt="" style={{ width: 24, height: 24, borderRadius: '50%' }} />
              : <div style={{ width: 24, height: 24, borderRadius: '50%', background: 'var(--bg3)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12 }}>👤</div>
            }
            <span style={{ fontWeight: 500 }}>{post.authorName}</span>
            <ProviderBadge provider={post.authorProvider} />
          </div>
          {post.characterName && <span className="text-small text-muted">캐릭터: {post.characterName}</span>}
          <span className="text-small text-muted">조회 {post.views}</span>
          <span className="text-small text-muted">{fmt(post.createdAt)}</span>
        </div>

        {/* 파티 정보 박스 */}
        <div style={{ background: 'var(--bg3)', borderRadius: 8, padding: '14px 16px', marginBottom: 16 }}>
          <div className="grid-3" style={{ gap: 8 }}>
            <div className="text-center">
              <div className="text-muted text-small mb-4">모집 인원</div>
              <div style={{ fontWeight: 700, fontSize: 18, color: post.currentMembers >= post.maxMembers ? 'var(--red1)' : 'var(--green1)' }}>
                {post.currentMembers} / {post.maxMembers}
              </div>
            </div>
            <div className="text-center">
              <div className="text-muted text-small mb-4">서버</div>
              <div style={{ fontWeight: 500 }}>{post.server || '-'}</div>
            </div>
            <div className="text-center">
              <div className="text-muted text-small mb-4">최소 레벨</div>
              <div style={{ fontWeight: 500 }}>{post.minLevel ? `Lv.${post.minLevel}` : '제한 없음'}</div>
            </div>
          </div>
          {post.scheduleTime && (
            <div className="text-center mt-8 text-small text-muted">
              예정 시각: {fmt(post.scheduleTime)}
            </div>
          )}
        </div>

        {/* 본문 */}
        <div style={{ fontSize: 14, lineHeight: 1.9, color: 'var(--text1)', whiteSpace: 'pre-wrap', marginBottom: 16 }}>
          {post.content}
        </div>

        {/* 작성자 버튼 */}
        {(isAuthor || isAdmin) && (
          <div className="flex gap-8 mt-16" style={{ justifyContent: 'flex-end' }}>
            {isAuthor && post.status === 'OPEN' && (
              <button className="btn btn-ghost btn-sm" onClick={() => navigate(`/party/${id}/edit`)}>수정</button>
            )}
            {isAuthor && post.status === 'OPEN' && (
              <button className="btn btn-ghost btn-sm" onClick={handleClose}>모집 마감</button>
            )}
            {(isAuthor || isAdmin) && (
              <button className="btn btn-danger btn-sm" onClick={handleDelete}>삭제</button>
            )}
          </div>
        )}
      </div>

      {/* 댓글 */}
      <div className="card">
        <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16 }}>댓글 {post.comments?.length || 0}</h3>

        {post.comments?.length === 0 && (
          <p className="text-muted text-small text-center" style={{ padding: '16px 0' }}>첫 댓글을 작성해보세요</p>
        )}

        {post.comments?.map(c => (
          <div key={c.id} style={{ display: 'flex', gap: 10, padding: '10px 0', borderBottom: '1px solid var(--border)' }}>
            <div style={{ width: 28, height: 28, borderRadius: '50%', background: 'var(--bg3)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12, flexShrink: 0 }}>👤</div>
            <div style={{ flex: 1 }}>
              <div className="flex items-center gap-6 mb-4">
                <span style={{ fontWeight: 500, fontSize: 13 }}>{c.authorName}</span>
                <ProviderBadge provider={c.authorProvider} />
                <span className="text-small text-muted">{fmt(c.createdAt)}</span>
                {(session?.id === c.accountId || isAdmin) && (
                  <button className="btn btn-xs btn-danger" style={{ marginLeft: 'auto' }} onClick={() => handleDelComment(c.id)}>삭제</button>
                )}
              </div>
              <p style={{ fontSize: 13, color: 'var(--text2)', lineHeight: 1.6 }}>{c.content}</p>
            </div>
          </div>
        ))}

        {/* 댓글 입력 */}
        {session ? (
          <form onSubmit={handleComment} className="mt-16">
            <textarea className="textarea" placeholder="댓글을 입력하세요" value={comment}
              onChange={e => setComment(e.target.value)} style={{ minHeight: 80, marginBottom: 8 }} />
            <div style={{ textAlign: 'right' }}>
              <button className="btn btn-primary btn-sm" disabled={submitting || !comment.trim()}>
                {submitting ? '등록 중...' : '댓글 등록'}
              </button>
            </div>
          </form>
        ) : (
          <div className="text-center mt-16">
            <p className="text-muted text-small mb-8">댓글을 작성하려면 로그인이 필요합니다</p>
            <button className="btn btn-ghost btn-sm" onClick={() => navigate('/login')}>로그인</button>
          </div>
        )}
      </div>
    </div>
  )
}
