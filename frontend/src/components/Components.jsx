import React, { useEffect, useRef, useState } from 'react'
import { useAuthCtx } from '../App.jsx'

// ── AdBanner ──────────────────────────────────────────────────────────────────
export function AdBanner({ slot }) {
  useEffect(() => { try { (window.adsbygoogle = window.adsbygoogle || []).push({}) } catch (e) {} }, [])
  return (
    <div className="ad-banner">
      [광고 영역 — AdSense 승인 후 표시됩니다]
    </div>
  )
}

// ── Pagination ────────────────────────────────────────────────────────────────
export function Pagination({ current, total, onChange }) {
  if (total <= 1) return null
  return (
    <div className="flex items-center gap-8 mt-16" style={{ justifyContent: 'center' }}>
      <button className="btn btn-ghost btn-sm" disabled={current === 0} onClick={() => onChange(current - 1)}>← 이전</button>
      <span className="text-muted text-small">{current + 1} / {total}</span>
      <button className="btn btn-ghost btn-sm" disabled={current >= total - 1} onClick={() => onChange(current + 1)}>다음 →</button>
    </div>
  )
}

// ── VerificationModal ─────────────────────────────────────────────────────────
export function VerificationModal({ char, onClose, onVerified }) {
  const { api } = useAuthCtx()
  const [step, setStep]           = useState('idle')
  const [issueData, setIssueData] = useState(null)
  const [message, setMessage]     = useState('')
  const [countdown, setCountdown] = useState(0)
  const pollRef  = useRef(null)
  const timerRef = useRef(null)

  useEffect(() => {
    if (!issueData) return
    timerRef.current = setInterval(() => {
      const left = Math.max(0, Math.round((new Date(issueData.expiresAt) - new Date()) / 1000))
      setCountdown(left)
      if (left === 0) { clearInterval(timerRef.current); clearInterval(pollRef.current); setStep('idle'); setMessage('코드가 만료되었습니다. 다시 발급해주세요.') }
    }, 1000)
    return () => clearInterval(timerRef.current)
  }, [issueData])

  useEffect(() => () => { clearInterval(pollRef.current); clearInterval(timerRef.current) }, [])

  const issueCode = async () => {
    setStep('issuing'); setMessage('')
    try { const r = await api.post(`/characters/${char.id}/verification/issue`); setIssueData(r); setStep('issued') }
    catch (e) { setMessage(e.message); setStep('idle') }
  }

  const startVerify = () => {
    setStep('verifying')
    const poll = async () => {
      try {
        const r = await api.post(`/characters/${char.id}/verification/verify`)
        if (r.success) { clearInterval(pollRef.current); setStep('done'); setMessage(r.message); onVerified(char.id) }
      } catch (e) {
        if (e.message.includes('만료')) { clearInterval(pollRef.current); setStep('idle'); setMessage('코드가 만료되었습니다.') }
      }
    }
    poll(); pollRef.current = setInterval(poll, 10000)
  }

  const mm = Math.floor(countdown / 60), ss = String(countdown % 60).padStart(2, '0')

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal-box">
        <button className="modal-close" onClick={onClose}>×</button>
        <h3 style={{ marginBottom: 4 }}>캐릭터 인증</h3>
        <p className="text-muted text-small mb-16">{char.server} — {char.name}</p>

        {step === 'idle' && (
          <>
            <p className="text-muted mb-16" style={{ fontSize: 13 }}>아이온2 공식 게시글에 인증 코드를 댓글로 작성하면 캐릭터 소유권이 확인됩니다.</p>
            <button className="btn btn-primary w-full" onClick={issueCode}>인증 코드 발급받기</button>
            {message && <p className="text-danger text-small mt-8">{message}</p>}
          </>
        )}
        {step === 'issuing' && <div className="text-center"><div className="spinner" /></div>}
        {step === 'issued' && issueData && (
          <>
            <div className="code-display">
              <span className="code-text">{issueData.code}</span>
              <button className="btn btn-ghost btn-sm" onClick={() => navigator.clipboard.writeText(issueData.code)}>복사</button>
            </div>
            <p className="text-center text-small text-muted mb-16">만료까지 {mm}:{ss}</p>
            <ol style={{ fontSize: 13, color: 'var(--text2)', paddingLeft: 20, marginBottom: 20, lineHeight: 2 }}>
              <li>위 코드를 복사하세요</li>
              <li><a href={issueData.articleUrl} target="_blank" rel="noopener noreferrer">공식 게시글 댓글 작성하러 가기 ↗</a></li>
              <li>댓글에 코드를 붙여넣고 게시하세요</li>
            </ol>
            <button className="btn btn-gold w-full" onClick={startVerify}>댓글 작성 완료 — 인증 확인</button>
          </>
        )}
        {step === 'verifying' && (
          <div className="text-center" style={{ padding: '20px 0' }}>
            <div className="spinner" />
            <p className="text-muted text-small mt-8">댓글에서 코드 확인 중... (10초마다 자동 재시도)</p>
          </div>
        )}
        {step === 'done' && (
          <div className="text-center" style={{ padding: '16px 0' }}>
            <div style={{ fontSize: 48, color: 'var(--green1)', marginBottom: 12 }}>✓</div>
            <p className="text-success" style={{ fontWeight: 600, fontSize: 16, marginBottom: 8 }}>인증 완료!</p>
            <p className="text-muted text-small mb-16">{message}</p>
            <button className="btn btn-ghost" onClick={onClose}>닫기</button>
          </div>
        )}
      </div>
    </div>
  )
}

// ── ProviderBadge ─────────────────────────────────────────────────────────────
export function ProviderBadge({ provider }) {
  const map = { LOCAL: { label: '일반', cls: 'badge-blue' }, KAKAO: { label: '카카오', cls: 'badge-gold' }, NAVER: { label: '네이버', cls: 'badge-green' }, GOOGLE: { label: '구글', cls: 'badge-red' }, ADMIN: { label: '관리자', cls: 'badge-purple' } }
  const info = map[provider] || { label: provider, cls: 'badge-gray' }
  return <span className={`badge ${info.cls}`}>{info.label}</span>
}

// ── ServerSelect ──────────────────────────────────────────────────────────────
export function ServerSelect({ value, onChange, className = 'select' }) {
  return (
    <select className={className} value={value} onChange={e => onChange(e.target.value)}>
      <option value="">서버 선택</option>
      {['카이나토스', '에이온', '아트레이아'].map(s => <option key={s} value={s}>{s}</option>)}
    </select>
  )
}
