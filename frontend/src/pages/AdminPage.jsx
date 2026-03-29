import React, { useState, useEffect, useCallback } from 'react'
import { useAuthCtx } from '../App.jsx'

export default function AdminPage() {
  const { api } = useAuthCtx()
  const [tab, setTab] = useState('accounts')

  return (
    <div className="page">
      <h1 className="page-title">관리자 패널</h1>
      <div className="tabs">
        {[
          { key: 'accounts', label: '계정 관리' },
          { key: 'quests',   label: '퀘스트 관리' },
          { key: 'config',   label: '인증 설정' },
        ].map(t => (
          <button key={t.key}
            className={`tab-btn ${tab === t.key ? 'active' : ''}`}
            onClick={() => setTab(t.key)}>
            {t.label}
          </button>
        ))}
      </div>
      {tab === 'accounts' && <AccountsTab api={api} />}
      {tab === 'quests'   && <QuestsTab   api={api} />}
      {tab === 'config'   && <ConfigTab   api={api} />}
    </div>
  )
}

// ── 계정 관리 탭 ──────────────────────────────────────────────────────────────
function AccountsTab({ api }) {
  const [accounts, setAccounts] = useState([])
  const [form, setForm]         = useState({ username: '', password: '' })
  const [error, setError]       = useState('')
  const [loading, setLoading]   = useState(true)

  const load = useCallback(() => {
    api.get('/admin/accounts').then(setAccounts).finally(() => setLoading(false))
  }, [api])
  useEffect(() => { load() }, [load])

  const handleCreate = async e => {
    e.preventDefault(); setError('')
    try {
      await api.post('/admin/accounts', form)
      setForm({ username: '', password: '' }); load()
    } catch (err) { setError(err.message) }
  }

  return (
    <>
      <div className="card mb-16">
        <h3 style={{ marginBottom: 14, fontSize: 14 }}>새 계정 생성</h3>
        <form onSubmit={handleCreate} className="flex gap-8">
          <input className="input" placeholder="아이디"
            value={form.username}
            onChange={e => setForm(p => ({ ...p, username: e.target.value }))} />
          <input className="input" type="password" placeholder="비밀번호"
            value={form.password}
            onChange={e => setForm(p => ({ ...p, password: e.target.value }))} />
          <button className="btn btn-primary" type="submit">생성</button>
        </form>
        {error && <p className="text-danger text-small mt-8">{error}</p>}
      </div>
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        {loading ? <div className="spinner" /> : (
          <table className="table">
            <thead><tr><th>#</th><th>아이디</th><th>역할</th><th>가입일</th></tr></thead>
            <tbody>
              {accounts.map(a => (
                <tr key={a.id}>
                  <td>{a.id}</td>
                  <td style={{ fontWeight: 500 }}>{a.username}</td>
                  <td><span className={`badge ${a.role === 'ADMIN' ? 'badge-gold' : 'badge-gray'}`}>{a.role}</span></td>
                  <td className="text-muted text-small">{new Date(a.createdAt).toLocaleDateString('ko-KR')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}

// ── 퀘스트 관리 탭 ────────────────────────────────────────────────────────────
const DAY_OPTIONS = [
  { value: 1, label: '월요일' }, { value: 2, label: '화요일' },
  { value: 3, label: '수요일' }, { value: 4, label: '목요일' },
  { value: 5, label: '금요일' }, { value: 6, label: '토요일' },
  { value: 7, label: '일요일' },
]
const HOUR_OPTIONS = Array.from({ length: 24 }, (_, i) => ({
  value: i,
  label: i === 0 ? '자정 (0시)' : `오전 ${i < 12 ? i : i - 12 || 12}시${i >= 12 ? ' (오후)' : ''}`,
}))

function QuestsTab({ api }) {
  const [quests, setQuests]   = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState('')
  const INIT = { name: '', type: 'DAILY', minLevel: '', maxLevel: '', resetDay: 3, resetHour: 5 }
  const [form, setForm]       = useState(INIT)

  const load = useCallback(() => {
    api.get('/admin/quests').then(setQuests).finally(() => setLoading(false))
  }, [api])
  useEffect(() => { load() }, [load])

  const handleCreate = async e => {
    e.preventDefault(); setError('')
    try {
      await api.post('/admin/quests', {
        ...form,
        minLevel:  form.minLevel  ? parseInt(form.minLevel)  : null,
        maxLevel:  form.maxLevel  ? parseInt(form.maxLevel)  : null,
        resetDay:  parseInt(form.resetDay),
        resetHour: parseInt(form.resetHour),
      })
      setForm(INIT); load()
    } catch (err) { setError(err.message) }
  }

  const isWeekly = form.type === 'WEEKLY'

  return (
    <>
      <div className="card mb-16">
        <h3 style={{ marginBottom: 14, fontSize: 14 }}>퀘스트 템플릿 추가</h3>
        <form onSubmit={handleCreate}>
          {/* 이름 + 유형 */}
          <div className="grid-2" style={{ marginBottom: 10 }}>
            <div className="form-group">
              <label className="label">퀘스트명</label>
              <input className="input" placeholder="퀘스트명"
                value={form.name}
                onChange={e => setForm(p => ({ ...p, name: e.target.value }))} />
            </div>
            <div className="form-group">
              <label className="label">유형</label>
              <select className="select" value={form.type}
                onChange={e => setForm(p => ({ ...p, type: e.target.value }))}>
                <option value="DAILY">일일 (매일 초기화)</option>
                <option value="WEEKLY">주간 (매주 초기화)</option>
                <option value="SPECIFIC">특정 (수동 관리)</option>
              </select>
            </div>
          </div>

          {/* 초기화 시간 설정 */}
          <div className="card" style={{ background: 'var(--color-background-secondary, #f8fafc)', marginBottom: 10 }}>
            <div style={{ fontSize: 12, fontWeight: 600, color: 'var(--color-text-secondary)', marginBottom: 10 }}>
              초기화 시간 설정
            </div>
            <div className="grid-2">
              {/* 주간만 요일 표시 */}
              {isWeekly && (
                <div className="form-group">
                  <label className="label">초기화 요일</label>
                  <select className="select" value={form.resetDay}
                    onChange={e => setForm(p => ({ ...p, resetDay: parseInt(e.target.value) }))}>
                    {DAY_OPTIONS.map(d => (
                      <option key={d.value} value={d.value}>{d.label}</option>
                    ))}
                  </select>
                </div>
              )}
              <div className="form-group">
                <label className="label">초기화 시각</label>
                <select className="select" value={form.resetHour}
                  onChange={e => setForm(p => ({ ...p, resetHour: parseInt(e.target.value) }))}>
                  {Array.from({ length: 24 }, (_, i) => (
                    <option key={i} value={i}>
                      오전 {i < 12 ? i : i === 12 ? 12 : i - 12}시 {i >= 12 ? '(오후)' : ''}
                      {i === 0 ? ' (자정)' : ''}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <p className="text-small" style={{ color: 'var(--color-text-tertiary)', marginTop: 4 }}>
              {isWeekly
                ? `매주 ${DAY_OPTIONS.find(d => d.value === parseInt(form.resetDay))?.label} 오전 ${form.resetHour}시에 초기화됩니다`
                : `매일 오전 ${form.resetHour}시에 초기화됩니다`}
            </p>
          </div>

          {/* 레벨 범위 */}
          <div className="grid-2" style={{ marginBottom: 10 }}>
            <div className="form-group">
              <label className="label">최소 레벨 (선택)</label>
              <input className="input" type="number" placeholder="제한 없음"
                value={form.minLevel}
                onChange={e => setForm(p => ({ ...p, minLevel: e.target.value }))} />
            </div>
            <div className="form-group">
              <label className="label">최대 레벨 (선택)</label>
              <input className="input" type="number" placeholder="제한 없음"
                value={form.maxLevel}
                onChange={e => setForm(p => ({ ...p, maxLevel: e.target.value }))} />
            </div>
          </div>
          {error && <p className="text-danger text-small mb-8">{error}</p>}
          <button className="btn btn-primary" type="submit">추가</button>
        </form>
      </div>

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        {loading ? <div className="spinner" /> : (
          <table className="table">
            <thead>
              <tr>
                <th>#</th><th>퀘스트명</th><th>유형</th><th>레벨 범위</th><th>초기화 주기</th>
              </tr>
            </thead>
            <tbody>
              {quests.map(q => (
                <tr key={q.id}>
                  <td>{q.id}</td>
                  <td style={{ fontWeight: 500 }}>{q.name}</td>
                  <td>
                    <span className={`badge ${
                      q.type === 'DAILY'   ? 'badge-blue'   :
                      q.type === 'WEEKLY'  ? 'badge-purple' : 'badge-gray'
                    }`}>
                      {q.type === 'DAILY' ? '일일' : q.type === 'WEEKLY' ? '주간' : '특정'}
                    </span>
                  </td>
                  <td className="text-muted text-small">
                    {q.minLevel || '-'} ~ {q.maxLevel || '∞'}
                  </td>
                  <td className="text-small" style={{ color: 'var(--color-text-secondary)' }}>
                    {q.resetInfo}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}

// ── 인증 설정 탭 ──────────────────────────────────────────────────────────────
function ConfigTab({ api }) {
  const [config, setConfig]   = useState({ article_url: '', code_expire_minutes: '10', code_prefix: 'AION' })
  const [saved, setSaved]     = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/admin/config/verify').then(setConfig).finally(() => setLoading(false))
  }, [api])

  const handleSave = async e => {
    e.preventDefault()
    await api.put('/admin/config/verify', config)
    setSaved(true); setTimeout(() => setSaved(false), 3000)
  }

  if (loading) return <div className="spinner" />
  return (
    <div className="card" style={{ maxWidth: 560 }}>
      <h3 style={{ marginBottom: 16, fontSize: 14 }}>캐릭터 인증 설정</h3>
      <form onSubmit={handleSave}>
        <div className="form-group">
          <label className="label">인증 게시글 URL</label>
          <input className="input" value={config.article_url}
            onChange={e => setConfig(p => ({ ...p, article_url: e.target.value }))}
            placeholder="https://aion2.plaync.com/..." />
          <p className="text-muted text-small mt-4">
            게시글이 바뀌면 여기서만 변경하면 됩니다
          </p>
        </div>
        <div className="grid-2">
          <div className="form-group">
            <label className="label">코드 만료 시간 (분)</label>
            <input className="input" type="number" value={config.code_expire_minutes}
              onChange={e => setConfig(p => ({ ...p, code_expire_minutes: e.target.value }))} />
          </div>
          <div className="form-group">
            <label className="label">코드 접두사</label>
            <input className="input" value={config.code_prefix}
              onChange={e => setConfig(p => ({ ...p, code_prefix: e.target.value }))} />
          </div>
        </div>
        <button className="btn btn-primary" type="submit">설정 저장</button>
        {saved && <span className="text-success text-small" style={{ marginLeft: 12 }}>✓ 저장됨</span>}
      </form>
    </div>
  )
}
