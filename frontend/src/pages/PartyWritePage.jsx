import React, { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { publicApi } from '../api/index.js'
import { useAuthCtx } from '../App.jsx'

export default function PartyWritePage() {
  const { id } = useParams()   // id가 있으면 수정 모드
  const navigate = useNavigate()
  const { api, session } = useAuthCtx()

  const [categories, setCategories] = useState([])
  const [characters, setCharacters] = useState([])
  const [loading, setLoading]       = useState(false)
  const [error, setError]           = useState('')

  const [form, setForm] = useState({
    categoryId: '', subcategoryId: '', characterId: '',
    title: '', content: '', server: '',
    minLevel: '', maxMembers: 4, scheduleTime: '',
  })

  const SERVERS = ['카이나토스', '에이온', '아트레이아']

  useEffect(() => {
    publicApi.get('/party/categories').then(setCategories).catch(() => {})
    api.get('/characters').then(setCharacters).catch(() => {})
  }, [])

  // 수정 모드: 기존 데이터 로드
  useEffect(() => {
    if (!id) return
    publicApi.get(`/party/posts/${id}`).then(post => {
      if (String(post.accountId) !== String(session?.id)) { navigate('/party'); return }
      setForm({
        categoryId:    post.categoryId    || '',
        subcategoryId: post.subcategoryId || '',
        characterId:   post.characterId   || '',
        title:         post.title,
        content:       post.content,
        server:        post.server,
        minLevel:      post.minLevel      || '',
        maxMembers:    post.maxMembers,
        scheduleTime:  post.scheduleTime  ? post.scheduleTime.slice(0, 16) : '',
      })
    }).catch(() => navigate('/party'))
  }, [id])

  const currentSubs = form.categoryId
    ? (categories.find(c => String(c.id) === String(form.categoryId))?.subcategories || [])
    : []

  const handleSubmit = async e => {
    e.preventDefault(); setError(''); setLoading(true)
    if (!form.categoryId || !form.subcategoryId || !form.title || !form.content || !form.server) {
      setError('카테고리, 제목, 내용, 서버는 필수입니다'); setLoading(false); return
    }
    const payload = {
      categoryId:    Number(form.categoryId),
      subcategoryId: Number(form.subcategoryId),
      characterId:   form.characterId ? Number(form.characterId) : null,
      title:         form.title,
      content:       form.content,
      server:        form.server,
      minLevel:      form.minLevel ? Number(form.minLevel) : null,
      maxMembers:    Number(form.maxMembers),
      scheduleTime:  form.scheduleTime || null,
    }
    try {
      const result = id
        ? await api.put(`/party/posts/${id}`, payload)
        : await api.post('/party/posts', payload)
      navigate(`/party/${result.id}`)
    } catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }

  return (
    <div className="page" style={{ maxWidth: 720 }}>
      <button className="btn btn-ghost btn-sm mb-16" onClick={() => navigate(-1)}>← 뒤로</button>
      <h1 className="page-title">{id ? '모집 글 수정' : '파티 모집 글 작성'}</h1>

      <form onSubmit={handleSubmit}>
        <div className="card mb-12">
          <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 14 }}>카테고리</h3>
          <div className="grid-2">
            <div className="form-group">
              <label className="label">1차 카테고리 *</label>
              <select className="select" value={form.categoryId}
                onChange={e => setForm(p => ({ ...p, categoryId: e.target.value, subcategoryId: '' }))}>
                <option value="">선택하세요</option>
                {categories.map(c => <option key={c.id} value={c.id}>{c.icon} {c.name}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="label">2차 카테고리 *</label>
              <select className="select" value={form.subcategoryId}
                onChange={e => setForm(p => ({ ...p, subcategoryId: e.target.value }))}
                disabled={!form.categoryId}>
                <option value="">선택하세요</option>
                {currentSubs.map(s => <option key={s.id} value={s.id}>{s.name}{s.minLevel ? ` (Lv.${s.minLevel}+)` : ''}</option>)}
              </select>
            </div>
          </div>
        </div>

        <div className="card mb-12">
          <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 14 }}>파티 정보</h3>
          <div className="form-group">
            <label className="label">제목 *</label>
            <input className="input" placeholder="모집 글 제목을 입력하세요" value={form.title}
              onChange={e => setForm(p => ({ ...p, title: e.target.value }))} />
          </div>
          <div className="grid-2">
            <div className="form-group">
              <label className="label">서버 *</label>
              <select className="select" value={form.server}
                onChange={e => setForm(p => ({ ...p, server: e.target.value }))}>
                <option value="">선택하세요</option>
                {SERVERS.map(s => <option key={s} value={s}>{s}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="label">대표 캐릭터 (선택)</label>
              <select className="select" value={form.characterId}
                onChange={e => setForm(p => ({ ...p, characterId: e.target.value }))}>
                <option value="">선택 안 함</option>
                {characters.map(c => <option key={c.id} value={c.id}>{c.name} ({c.server}) Lv.{c.level}</option>)}
              </select>
            </div>
          </div>
          <div className="grid-2">
            <div className="form-group">
              <label className="label">모집 인원</label>
              <select className="select" value={form.maxMembers}
                onChange={e => setForm(p => ({ ...p, maxMembers: e.target.value }))}>
                {[2,3,4,5,6,7,8,10,12,16,24].map(n => <option key={n} value={n}>{n}명</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="label">최소 레벨 (선택)</label>
              <input className="input" type="number" placeholder="제한 없음" value={form.minLevel}
                onChange={e => setForm(p => ({ ...p, minLevel: e.target.value }))} />
            </div>
          </div>
          <div className="form-group">
            <label className="label">예정 시각 (선택)</label>
            <input className="input" type="datetime-local" value={form.scheduleTime}
              onChange={e => setForm(p => ({ ...p, scheduleTime: e.target.value }))} />
          </div>
        </div>

        <div className="card mb-12">
          <div className="form-group">
            <label className="label">상세 내용 *</label>
            <textarea className="textarea" placeholder="파티 모집 내용을 자세히 적어주세요. (요구 스펙, 전투력, 역할 등)" value={form.content}
              onChange={e => setForm(p => ({ ...p, content: e.target.value }))} style={{ minHeight: 200 }} />
          </div>
        </div>

        {error && <p className="text-danger text-small mb-8">{error}</p>}
        <div className="flex gap-8">
          <button className="btn btn-ghost" type="button" onClick={() => navigate(-1)}>취소</button>
          <button className="btn btn-primary flex-1" disabled={loading}>
            {loading ? '처리 중...' : id ? '수정 완료' : '모집 글 등록'}
          </button>
        </div>
      </form>
    </div>
  )
}
