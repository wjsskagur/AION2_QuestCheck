import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuthCtx } from '../App.jsx'
import { VerificationModal } from '../components/Components.jsx'

// ── Dashboard ─────────────────────────────────────────────────────────────────
export function Dashboard() {
  const { api } = useAuthCtx()
  const navigate = useNavigate()
  const [dashboard, setDashboard] = useState([])
  const [loading, setLoading]     = useState(true)
  const [toggling, setToggling]   = useState(null)

  const load = useCallback(() => { api.get('/quest-checks/dashboard').then(setDashboard).finally(() => setLoading(false)) }, [api])
  useEffect(() => { load() }, [load])

  const toggle = async (cid, qid) => {
    const key = `${cid}-${qid}`; setToggling(key)
    try { await api.post(`/quest-checks/${cid}/toggle/${qid}`); load() }
    finally { setToggling(null) }
  }

  if (loading) return <div className="page"><div className="spinner" /></div>

  return (
    <div className="page">
      <div className="flex items-center justify-between mb-16">
        <div><h1 className="page-title">대시보드</h1><p className="page-sub">오늘의 퀘스트 현황</p></div>
        <button className="btn btn-ghost btn-sm" onClick={() => navigate('/characters')}>캐릭터 관리</button>
      </div>

      {dashboard.length === 0 ? (
        <div className="empty-state">
          <p style={{ fontSize: 32 }}>⚔</p>
          <p>등록된 캐릭터가 없습니다</p>
          <button className="btn btn-primary mt-16" onClick={() => navigate('/characters')}>캐릭터 추가하기</button>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {dashboard.map(ch => <CharQuestCard key={ch.characterId} ch={ch} toggle={toggle} toggling={toggling} />)}
        </div>
      )}
    </div>
  )
}

function CharQuestCard({ ch, toggle, toggling }) {
  const pct = ch.totalCount > 0 ? Math.round((ch.doneCount / ch.totalCount) * 100) : 0
  const daily  = ch.quests.filter(q => q.type === 'DAILY')
  const weekly = ch.quests.filter(q => q.type === 'WEEKLY')
  return (
    <div className="card">
      <div className="flex items-center justify-between mb-12">
        <div className="flex items-center gap-8">
          <span style={{ fontSize: 16, fontWeight: 700 }}>{ch.characterName}</span>
          <span className="text-muted text-small">{ch.server}</span>
          <span className="badge badge-gray">Lv.{ch.level}</span>
          {ch.verified && <span className="verified-badge">✓ 인증</span>}
        </div>
        <span style={{ fontWeight: 600, color: pct === 100 ? 'var(--green1)' : 'var(--text2)' }}>{ch.doneCount}/{ch.totalCount}</span>
      </div>
      <div className="progress-wrap mb-16">
        <div className="progress-bar" style={{ width: pct + '%', background: pct === 100 ? 'var(--green1)' : 'var(--blue1)' }} />
      </div>
      {daily.length > 0 && (
        <><div className="text-small text-muted mb-4" style={{ fontWeight: 600 }}>일일 퀘스트</div>
        {daily.map(q => <QuestItem key={q.questId} q={q} charId={ch.characterId} toggle={toggle} toggling={toggling} />)}</>
      )}
      {weekly.length > 0 && (
        <div className="mt-12">
          <div className="text-small text-muted mb-4" style={{ fontWeight: 600 }}>주간 퀘스트</div>
          {weekly.map(q => <QuestItem key={q.questId} q={q} charId={ch.characterId} toggle={toggle} toggling={toggling} />)}
        </div>
      )}
      {ch.quests.length === 0 && <p className="text-muted text-small text-center" style={{ padding: '12px 0' }}>해당 레벨의 퀘스트가 없습니다</p>}
    </div>
  )
}

function QuestItem({ q, charId, toggle, toggling }) {
  const key = `${charId}-${q.questId}`
  return (
    <div className="quest-item" onClick={() => toggling !== key && toggle(charId, q.questId)}>
      <div className={`quest-check-box ${q.done ? 'checked' : ''}`}>{q.done && <span style={{ color: '#111', fontSize: 12, fontWeight: 700 }}>✓</span>}</div>
      <span className={`quest-name ${q.done ? 'done' : ''}`}>{q.name}</span>
      <span className="text-small text-muted" style={{ fontSize: 11 }}>{q.resetInfo}</span>
      {toggling === key && <span className="text-small text-muted">...</span>}
    </div>
  )
}

// ── CharactersPage ────────────────────────────────────────────────────────────
export function CharactersPage() {
  const { api } = useAuthCtx()
  const navigate = useNavigate()
  const [characters, setCharacters] = useState([])
  const [loading, setLoading]       = useState(true)
  const [addForm, setAddForm]        = useState({ server: '', name: '' })
  const [addError, setAddError]      = useState('')
  const [adding, setAdding]          = useState(false)
  const [verifyChar, setVerifyChar]  = useState(null)

  const load = useCallback(() => { api.get('/characters').then(setCharacters).finally(() => setLoading(false)) }, [api])
  useEffect(() => { load() }, [load])

  const handleAdd = async e => {
    e.preventDefault()
    if (!addForm.server || !addForm.name.trim()) { setAddError('서버와 캐릭터명을 입력해주세요'); return }
    setAdding(true); setAddError('')
    try { await api.post('/characters', addForm); setAddForm({ server: '', name: '' }); load() }
    catch (err) { setAddError(err.message) }
    finally { setAdding(false) }
  }

  if (loading) return <div className="page"><div className="spinner" /></div>

  return (
    <div className="page">
      <h1 className="page-title">캐릭터 관리</h1>
      <p className="page-sub">최대 16개 캐릭터를 등록할 수 있습니다</p>

      <div className="card mb-16">
        <h3 style={{ marginBottom: 14, fontSize: 14 }}>캐릭터 추가</h3>
        <form onSubmit={handleAdd} className="flex gap-8">
          <select className="select" style={{ flex: '0 0 140px' }} value={addForm.server} onChange={e => setAddForm(p => ({ ...p, server: e.target.value }))}>
            <option value="">서버 선택</option>
            {['카이나토스', '에이온', '아트레이아'].map(s => <option key={s} value={s}>{s}</option>)}
          </select>
          <input className="input" placeholder="캐릭터명" value={addForm.name} onChange={e => setAddForm(p => ({ ...p, name: e.target.value }))} />
          <button className="btn btn-primary" disabled={adding}>{adding ? '추가 중...' : '추가'}</button>
        </form>
        {addError && <p className="text-danger text-small mt-8">{addError}</p>}
      </div>

      {characters.length === 0 ? <div className="empty-state"><p>등록된 캐릭터가 없습니다</p></div> : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {characters.map(ch => (
            <div key={ch.id} className="card">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-8" style={{ cursor: 'pointer', flex: 1 }} onClick={() => navigate(`/characters/${ch.id}`)}>
                  <div>
                    <span style={{ fontWeight: 600 }}>{ch.name}</span>
                    <span className="text-muted text-small" style={{ marginLeft: 8 }}>{ch.server}</span>
                    {ch.verified && <span className="verified-badge" style={{ marginLeft: 6 }}>✓ 인증</span>}
                  </div>
                  <div className="flex gap-8"><span className="badge badge-gray">Lv.{ch.level}</span>{ch.grade && <span className="badge badge-gold">{ch.grade}</span>}</div>
                </div>
                <div className="flex gap-8">
                  {!ch.verified && <button className="btn btn-ghost btn-sm" onClick={() => setVerifyChar(ch)}>인증받기</button>}
                  <button className="btn btn-ghost btn-sm" onClick={() => api.put(`/characters/${ch.id}/refresh`).then(load)}>갱신</button>
                  <button className="btn btn-danger btn-sm" onClick={() => confirm('삭제하시겠습니까?') && api.delete(`/characters/${ch.id}`).then(load)}>삭제</button>
                </div>
              </div>
              {ch.combatPower > 0 && <div className="text-small text-muted mt-4">전투력 {ch.combatPower.toLocaleString()}</div>}
            </div>
          ))}
        </div>
      )}

      {verifyChar && <VerificationModal char={verifyChar} onClose={() => setVerifyChar(null)} onVerified={() => { setVerifyChar(null); load() }} />}
    </div>
  )
}

// ── CharDetailPage ────────────────────────────────────────────────────────────
export function CharDetailPage() {
  const { api } = useAuthCtx()
  const { id } = useParams()
  const navigate = useNavigate()
  const [char, setChar]   = useState(null)
  const [tab, setTab]     = useState('info')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/characters').then(list => { setChar(list.find(c => String(c.id) === String(id)) || null) }).finally(() => setLoading(false))
  }, [id])

  if (loading) return <div className="page"><div className="spinner" /></div>
  if (!char) return <div className="page"><div className="empty-state"><p>캐릭터를 찾을 수 없습니다</p><button className="btn btn-ghost mt-16" onClick={() => navigate('/characters')}>돌아가기</button></div></div>

  const raw = char.rawData ? JSON.parse(char.rawData) : null
  return (
    <div className="page" style={{ maxWidth: 720 }}>
      <button className="btn btn-ghost btn-sm mb-16" onClick={() => navigate('/characters')}>← 목록으로</button>
      <div className="card mb-16">
        <div className="flex gap-16">
          <div style={{ width: 72, height: 72, borderRadius: 8, background: 'var(--bg3)', border: '2px solid var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 28, flexShrink: 0 }}>⚔</div>
          <div style={{ flex: 1 }}>
            <div className="flex items-center gap-8 mb-4">
              <h2 style={{ fontSize: 20, fontWeight: 700 }}>{char.name}</h2>
              {char.verified && <span className="verified-badge">✓ 인증됨</span>}
            </div>
            <div className="flex gap-8 flex-wrap">
              <span className="badge badge-gray">{char.server}</span>
              {char.className && <span className="badge badge-purple">{char.className}</span>}
              <span className="badge badge-blue">Lv.{char.level}</span>
              {char.grade && <span className="badge badge-gold">{char.grade}</span>}
            </div>
            <div className="mt-8 text-small text-muted">
              전투력 <span style={{ color: 'var(--gold)', fontWeight: 600 }}>{char.combatPower.toLocaleString()}</span>
              {char.wings && <span style={{ marginLeft: 12 }}>날개: {char.wings}</span>}
            </div>
          </div>
        </div>
      </div>
      <div className="tabs">
        {[['info','기본 정보'],['raw','원본 데이터']].map(([k,l]) => (
          <button key={k} className={`tab-btn ${tab === k ? 'active' : ''}`} onClick={() => setTab(k)}>{l}</button>
        ))}
      </div>
      {tab === 'info' && (
        <div className="card">
          {[['캐릭터명',char.name],['서버',char.server],['직업',char.className||'-'],['레벨',`Lv.${char.level}`],['전투력',char.combatPower.toLocaleString()],['등급',char.grade||'-'],['날개',char.wings||'-'],['인증 상태',char.verified?'인증됨':'미인증']].map(([k,v]) => (
            <div key={k} className="flex items-center justify-between" style={{ padding: '10px 0', borderBottom: '1px solid var(--border)' }}>
              <span className="text-muted text-small">{k}</span>
              <span style={{ fontSize: 13, fontWeight: 500 }}>{v}</span>
            </div>
          ))}
        </div>
      )}
      {tab === 'raw' && (
        <div className="card">
          {raw ? <pre style={{ fontSize: 11, color: 'var(--text2)', overflow: 'auto', maxHeight: 400, lineHeight: 1.6 }}>{JSON.stringify(raw, null, 2)}</pre>
               : <p className="text-muted text-small text-center" style={{ padding: '20px 0' }}>공식 API 연동 후 표시됩니다</p>}
        </div>
      )}
    </div>
  )
}

export default Dashboard
