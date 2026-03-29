import React, { useState, useEffect, useCallback } from 'react'
import { useAuthCtx } from '../App.jsx'
import { ProviderBadge } from '../components/Components.jsx'

export default function AdminPage() {
  const { api } = useAuthCtx()
  const [tab, setTab] = useState('accounts')
  const TABS = [['accounts','계정 관리'],['quests','퀘스트 관리'],['party','파티 카테고리'],['config','인증 설정']]
  return (
    <div className="page">
      <h1 className="page-title">관리자 패널</h1>
      <div className="tabs">
        {TABS.map(([k,l]) => <button key={k} className={`tab-btn ${tab===k?'active':''}`} onClick={()=>setTab(k)}>{l}</button>)}
      </div>
      {tab === 'accounts' && <AccountsTab api={api} />}
      {tab === 'quests'   && <QuestsTab api={api} />}
      {tab === 'party'    && <PartyTab api={api} />}
      {tab === 'config'   && <ConfigTab api={api} />}
    </div>
  )
}

// ── 계정 관리 ─────────────────────────────────────────────────────────────────
function AccountsTab({ api }) {
  const [accounts, setAccounts] = useState([])
  const [form, setForm]         = useState({ username: '', password: '' })
  const [error, setError]       = useState('')
  const load = useCallback(() => { api.get('/admin/accounts').then(setAccounts) }, [api])
  useEffect(() => { load() }, [load])
  const handleCreate = async e => {
    e.preventDefault(); setError('')
    try { await api.post('/admin/accounts', form); setForm({ username: '', password: '' }); load() }
    catch (err) { setError(err.message) }
  }
  return (
    <>
      <div className="card mb-16">
        <h3 style={{ marginBottom: 14, fontSize: 14 }}>새 계정 생성</h3>
        <form onSubmit={handleCreate} className="flex gap-8">
          <input className="input" placeholder="아이디" value={form.username} onChange={e=>setForm(p=>({...p,username:e.target.value}))} />
          <input className="input" type="password" placeholder="비밀번호" value={form.password} onChange={e=>setForm(p=>({...p,password:e.target.value}))} />
          <button className="btn btn-primary">생성</button>
        </form>
        {error && <p className="text-danger text-small mt-8">{error}</p>}
      </div>
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <table className="table">
          <thead><tr><th>#</th><th>아이디 / 닉네임</th><th>역할</th><th>가입 경로</th><th>가입일</th></tr></thead>
          <tbody>
            {accounts.map(a => (
              <tr key={a.id}>
                <td>{a.id}</td>
                <td><div style={{ fontWeight: 500 }}>{a.nickname || a.username || '-'}</div><div className="text-small text-muted">{a.username}</div></td>
                <td><span className={`badge ${a.role==='ADMIN'?'badge-gold':'badge-gray'}`}>{a.role}</span></td>
                <td><ProviderBadge provider={a.authProvider} /></td>
                <td className="text-muted text-small">{new Date(a.createdAt).toLocaleDateString('ko-KR')}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}

// ── 퀘스트 관리 ───────────────────────────────────────────────────────────────
function QuestsTab({ api }) {
  const [quests, setQuests]     = useState([])
  const [error, setError]       = useState('')
  const INIT = { name:'', type:'DAILY', minLevel:'', maxLevel:'', resetDay:3, resetHour:5 }
  const [form, setForm]         = useState(INIT)
  const load = useCallback(() => { api.get('/admin/quests').then(setQuests) }, [api])
  useEffect(() => { load() }, [load])
  const handleCreate = async e => {
    e.preventDefault(); setError('')
    try {
      await api.post('/admin/quests', { ...form, minLevel: form.minLevel?parseInt(form.minLevel):null, maxLevel: form.maxLevel?parseInt(form.maxLevel):null, resetDay: parseInt(form.resetDay), resetHour: parseInt(form.resetHour) })
      setForm(INIT); load()
    } catch (err) { setError(err.message) }
  }
  return (
    <>
      <div className="card mb-16">
        <h3 style={{ marginBottom: 14, fontSize: 14 }}>퀘스트 템플릿 추가</h3>
        <form onSubmit={handleCreate}>
          <div className="grid-2" style={{ marginBottom: 10 }}>
            <div className="form-group"><label className="label">퀘스트명</label><input className="input" placeholder="퀘스트명" value={form.name} onChange={e=>setForm(p=>({...p,name:e.target.value}))} /></div>
            <div className="form-group"><label className="label">유형</label><select className="select" value={form.type} onChange={e=>setForm(p=>({...p,type:e.target.value}))}><option value="DAILY">일일</option><option value="WEEKLY">주간</option><option value="SPECIFIC">특정</option></select></div>
          </div>
          <div className="grid-2" style={{ marginBottom: 10 }}>
            {form.type === 'WEEKLY' && <div className="form-group"><label className="label">초기화 요일</label><select className="select" value={form.resetDay} onChange={e=>setForm(p=>({...p,resetDay:parseInt(e.target.value)}))}>{[['1','월'],['2','화'],['3','수'],['4','목'],['5','금'],['6','토'],['7','일']].map(([v,l])=><option key={v} value={v}>{l}요일</option>)}</select></div>}
            <div className="form-group"><label className="label">초기화 시각</label><select className="select" value={form.resetHour} onChange={e=>setForm(p=>({...p,resetHour:parseInt(e.target.value)}))}>{Array.from({length:24},(_,i)=><option key={i} value={i}>오전 {i}시</option>)}</select></div>
          </div>
          <div className="grid-2" style={{ marginBottom: 10 }}>
            <div className="form-group"><label className="label">최소 레벨</label><input className="input" type="number" placeholder="제한 없음" value={form.minLevel} onChange={e=>setForm(p=>({...p,minLevel:e.target.value}))} /></div>
            <div className="form-group"><label className="label">최대 레벨</label><input className="input" type="number" placeholder="제한 없음" value={form.maxLevel} onChange={e=>setForm(p=>({...p,maxLevel:e.target.value}))} /></div>
          </div>
          {error && <p className="text-danger text-small mb-8">{error}</p>}
          <button className="btn btn-primary">추가</button>
        </form>
      </div>
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <table className="table">
          <thead><tr><th>#</th><th>퀘스트명</th><th>유형</th><th>레벨 범위</th><th>초기화 주기</th></tr></thead>
          <tbody>
            {quests.map(q => (
              <tr key={q.id}>
                <td>{q.id}</td>
                <td style={{ fontWeight: 500 }}>{q.name}</td>
                <td><span className={`badge ${q.type==='DAILY'?'badge-blue':q.type==='WEEKLY'?'badge-purple':'badge-gray'}`}>{q.type==='DAILY'?'일일':q.type==='WEEKLY'?'주간':'특정'}</span></td>
                <td className="text-muted text-small">{q.minLevel||'-'} ~ {q.maxLevel||'∞'}</td>
                <td className="text-small" style={{ color: 'var(--text2)' }}>{q.resetInfo}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}

// ── 파티 카테고리 관리 ────────────────────────────────────────────────────────
function PartyTab({ api }) {
  const [categories, setCategories] = useState([])
  const [selCat, setSelCat]         = useState(null)
  const [catForm, setCatForm]       = useState({ name: '', description: '', icon: '', sortOrder: 0 })
  const [subForm, setSubForm]       = useState({ name: '', description: '', minLevel: '', sortOrder: 0 })
  const [editCat, setEditCat]       = useState(null)
  const [editSub, setEditSub]       = useState(null)
  const [error, setError]           = useState('')

  const load = useCallback(() => {
    api.get('/party/categories').then(setCategories)
  }, [api])
  useEffect(() => { load() }, [load])

  const currentSubs = selCat ? (categories.find(c => String(c.id) === String(selCat))?.subcategories || []) : []

  const handleCreateCat = async e => {
    e.preventDefault(); setError('')
    try {
      await api.post('/admin/party/categories', { ...catForm, sortOrder: parseInt(catForm.sortOrder) || 0 })
      setCatForm({ name: '', description: '', icon: '', sortOrder: 0 }); load()
    } catch (err) { setError(err.message) }
  }

  const handleUpdateCat = async (id) => {
    try { await api.put(`/admin/party/categories/${id}`, editCat); setEditCat(null); load() }
    catch (err) { alert(err.message) }
  }

  const handleDeleteCat = async (id) => {
    if (!confirm('카테고리를 비활성화하시겠습니까? (하위 카테고리도 함께 비활성화됩니다)')) return
    try { await api.delete(`/admin/party/categories/${id}`); load() }
    catch (err) { alert(err.message) }
  }

  const handleCreateSub = async e => {
    e.preventDefault(); setError('')
    if (!selCat) { setError('1차 카테고리를 선택해주세요'); return }
    try {
      await api.post('/admin/party/subcategories', { ...subForm, categoryId: selCat, minLevel: subForm.minLevel ? parseInt(subForm.minLevel) : null, sortOrder: parseInt(subForm.sortOrder) || 0 })
      setSubForm({ name: '', description: '', minLevel: '', sortOrder: 0 }); load()
    } catch (err) { setError(err.message) }
  }

  const handleUpdateSub = async (id) => {
    try { await api.put(`/admin/party/subcategories/${id}`, { ...editSub, minLevel: editSub.minLevel ? parseInt(editSub.minLevel) : null }); setEditSub(null); load() }
    catch (err) { alert(err.message) }
  }

  const handleDeleteSub = async (id) => {
    if (!confirm('2차 카테고리를 비활성화하시겠습니까?')) return
    try { await api.delete(`/admin/party/subcategories/${id}`); load() }
    catch (err) { alert(err.message) }
  }

  return (
    <div className="grid-2" style={{ alignItems: 'flex-start' }}>
      {/* 1차 카테고리 */}
      <div>
        <div className="card mb-12">
          <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 12 }}>1차 카테고리 추가</h3>
          <form onSubmit={handleCreateCat}>
            <div className="form-group"><label className="label">이름 *</label><input className="input" placeholder="던전, 레이드, PvP 등" value={catForm.name} onChange={e=>setCatForm(p=>({...p,name:e.target.value}))} /></div>
            <div className="grid-2">
              <div className="form-group"><label className="label">아이콘 (이모지)</label><input className="input" placeholder="⚔" value={catForm.icon} onChange={e=>setCatForm(p=>({...p,icon:e.target.value}))} /></div>
              <div className="form-group"><label className="label">순서</label><input className="input" type="number" value={catForm.sortOrder} onChange={e=>setCatForm(p=>({...p,sortOrder:e.target.value}))} /></div>
            </div>
            <div className="form-group"><label className="label">설명</label><input className="input" placeholder="선택 입력" value={catForm.description} onChange={e=>setCatForm(p=>({...p,description:e.target.value}))} /></div>
            {error && <p className="text-danger text-small mb-8">{error}</p>}
            <button className="btn btn-primary btn-sm">추가</button>
          </form>
        </div>

        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          {categories.map(c => (
            <div key={c.id} style={{ padding: '12px 16px', borderBottom: '1px solid var(--border)', cursor: 'pointer', background: String(selCat) === String(c.id) ? 'var(--bg3)' : '' }}
              onClick={() => setSelCat(c.id)}>
              {editCat?.id === c.id ? (
                <div onClick={e=>e.stopPropagation()}>
                  <div className="flex gap-6 mb-6">
                    <input className="input" style={{ flex:1 }} value={editCat.name} onChange={e=>setEditCat(p=>({...p,name:e.target.value}))} />
                    <input className="input" style={{ width: 60 }} value={editCat.icon} onChange={e=>setEditCat(p=>({...p,icon:e.target.value}))} />
                    <input className="input" type="number" style={{ width: 60 }} value={editCat.sortOrder} onChange={e=>setEditCat(p=>({...p,sortOrder:parseInt(e.target.value)}))} />
                  </div>
                  <div className="flex gap-6">
                    <button className="btn btn-primary btn-xs" onClick={()=>handleUpdateCat(c.id)}>저장</button>
                    <button className="btn btn-ghost btn-xs" onClick={()=>setEditCat(null)}>취소</button>
                  </div>
                </div>
              ) : (
                <div className="flex items-center justify-between">
                  <span>{c.icon} {c.name}</span>
                  <div className="flex gap-6" onClick={e=>e.stopPropagation()}>
                    <button className="btn btn-ghost btn-xs" onClick={()=>setEditCat({...c})}>수정</button>
                    <button className="btn btn-danger btn-xs" onClick={()=>handleDeleteCat(c.id)}>삭제</button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* 2차 카테고리 */}
      <div>
        <div className="card mb-12">
          <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 4 }}>2차 카테고리 추가</h3>
          {selCat ? <p className="text-muted text-small mb-12">선택된 1차: {categories.find(c=>String(c.id)===String(selCat))?.name}</p>
                  : <p className="text-muted text-small mb-12">왼쪽에서 1차 카테고리를 선택하세요</p>}
          <form onSubmit={handleCreateSub}>
            <div className="form-group"><label className="label">이름 *</label><input className="input" placeholder="일반 던전, 하드 던전 등" value={subForm.name} onChange={e=>setSubForm(p=>({...p,name:e.target.value}))} disabled={!selCat} /></div>
            <div className="grid-2">
              <div className="form-group"><label className="label">최소 레벨</label><input className="input" type="number" placeholder="제한 없음" value={subForm.minLevel} onChange={e=>setSubForm(p=>({...p,minLevel:e.target.value}))} disabled={!selCat} /></div>
              <div className="form-group"><label className="label">순서</label><input className="input" type="number" value={subForm.sortOrder} onChange={e=>setSubForm(p=>({...p,sortOrder:e.target.value}))} disabled={!selCat} /></div>
            </div>
            <div className="form-group"><label className="label">설명</label><input className="input" placeholder="선택 입력" value={subForm.description} onChange={e=>setSubForm(p=>({...p,description:e.target.value}))} disabled={!selCat} /></div>
            <button className="btn btn-primary btn-sm" disabled={!selCat}>추가</button>
          </form>
        </div>

        {selCat && (
          <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
            {currentSubs.length === 0
              ? <p className="text-muted text-small text-center" style={{ padding: 16 }}>2차 카테고리가 없습니다</p>
              : currentSubs.map(s => (
                <div key={s.id} style={{ padding: '12px 16px', borderBottom: '1px solid var(--border)' }}>
                  {editSub?.id === s.id ? (
                    <div>
                      <div className="flex gap-6 mb-6">
                        <input className="input" style={{ flex:1 }} value={editSub.name} onChange={e=>setEditSub(p=>({...p,name:e.target.value}))} />
                        <input className="input" type="number" style={{ width: 80 }} placeholder="최소레벨" value={editSub.minLevel||''} onChange={e=>setEditSub(p=>({...p,minLevel:e.target.value}))} />
                        <input className="input" type="number" style={{ width: 60 }} value={editSub.sortOrder} onChange={e=>setEditSub(p=>({...p,sortOrder:parseInt(e.target.value)}))} />
                      </div>
                      <div className="flex gap-6">
                        <button className="btn btn-primary btn-xs" onClick={()=>handleUpdateSub(s.id)}>저장</button>
                        <button className="btn btn-ghost btn-xs" onClick={()=>setEditSub(null)}>취소</button>
                      </div>
                    </div>
                  ) : (
                    <div className="flex items-center justify-between">
                      <div>
                        <span style={{ fontWeight: 500 }}>{s.name}</span>
                        {s.minLevel && <span className="text-small text-muted" style={{ marginLeft: 8 }}>Lv.{s.minLevel}+</span>}
                      </div>
                      <div className="flex gap-6">
                        <button className="btn btn-ghost btn-xs" onClick={()=>setEditSub({...s})}>수정</button>
                        <button className="btn btn-danger btn-xs" onClick={()=>handleDeleteSub(s.id)}>삭제</button>
                      </div>
                    </div>
                  )}
                </div>
              ))
            }
          </div>
        )}
      </div>
    </div>
  )
}

// ── 인증 설정 ─────────────────────────────────────────────────────────────────
function ConfigTab({ api }) {
  const [config, setConfig] = useState({ article_url: '', code_expire_minutes: '10', code_prefix: 'AION' })
  const [saved, setSaved]   = useState(false)
  useEffect(() => { api.get('/admin/config/verify').then(setConfig) }, [api])
  const handleSave = async e => {
    e.preventDefault(); await api.put('/admin/config/verify', config); setSaved(true); setTimeout(() => setSaved(false), 3000)
  }
  return (
    <div className="card" style={{ maxWidth: 560 }}>
      <h3 style={{ marginBottom: 16, fontSize: 14 }}>캐릭터 인증 설정</h3>
      <form onSubmit={handleSave}>
        <div className="form-group"><label className="label">인증 게시글 URL</label><input className="input" value={config.article_url} onChange={e=>setConfig(p=>({...p,article_url:e.target.value}))} placeholder="https://aion2.plaync.com/..." /><p className="text-muted text-small mt-4">게시글이 바뀌면 여기서만 변경하면 됩니다</p></div>
        <div className="grid-2">
          <div className="form-group"><label className="label">코드 만료 시간 (분)</label><input className="input" type="number" value={config.code_expire_minutes} onChange={e=>setConfig(p=>({...p,code_expire_minutes:e.target.value}))} /></div>
          <div className="form-group"><label className="label">코드 접두사</label><input className="input" value={config.code_prefix} onChange={e=>setConfig(p=>({...p,code_prefix:e.target.value}))} /></div>
        </div>
        <button className="btn btn-primary">설정 저장</button>
        {saved && <span className="text-success text-small" style={{ marginLeft: 12 }}>✓ 저장됨</span>}
      </form>
    </div>
  )
}
