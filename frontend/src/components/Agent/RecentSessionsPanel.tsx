import { useEffect, useState } from 'react'
import { useAgentStore } from '../../store/useAgentStore'
import { getRecentSessions, getSessionMessages } from '../../api/agentApi'
import type { SessionSummary } from '../../api/agentApi'

function timeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime()
  const m = Math.floor(diff / 60000)
  if (m < 1) return 'только что'
  if (m < 60) return `${m} мин. назад`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h} ч. назад`
  return `${Math.floor(h / 24)} д. назад`
}

export default function RecentSessionsPanel() {
  const sessionId = useAgentStore((s) => s.sessionId)
  const setSessionId = useAgentStore((s) => s.setSessionId)
  const setMessages = useAgentStore((s) => s.setMessages)
  const setTaskState = useAgentStore((s) => s.setTaskState)
  const setTokenTotals = useAgentStore((s) => s.setTokenTotals)
  const setMemoryLayersSnapshot = useAgentStore((s) => s.setMemoryLayersSnapshot)

  const [sessions, setSessions] = useState<SessionSummary[]>([])
  const [loadingId, setLoadingId] = useState<string | null>(null)

  useEffect(() => {
    getRecentSessions(5).then(setSessions).catch(console.error)
  }, [sessionId])

  const handleSwitch = async (target: SessionSummary) => {
    if (target.sessionId === sessionId || loadingId) return
    setLoadingId(target.sessionId)
    try {
      const msgs = await getSessionMessages(target.sessionId)
      setMessages(msgs.map((m) => ({
        role: m.role,
        content: m.content,
        inputTokens: m.inputTokens,
        outputTokens: m.outputTokens,
        lastMessageId: m.messageId,
      })))
      setSessionId(target.sessionId)
      setTokenTotals({ totalInputTokens: 0, totalOutputTokens: 0, turnNumber: target.turnCount })
      setMemoryLayersSnapshot(null)
      setTaskState(null)
    } catch (e) {
      console.error(e)
    } finally {
      setLoadingId(null)
    }
  }

  const visible = sessions.filter((s) => s.sessionId !== sessionId || sessions.length === 1)
  if (visible.length === 0) return null

  return (
    <div className="flex flex-col gap-2 p-3 bg-gray-800 rounded-lg text-xs">
      <div className="text-gray-400 uppercase tracking-wider font-medium">Последние сессии</div>
      {visible.map((s) => (
        <button
          key={s.sessionId}
          onClick={() => handleSwitch(s)}
          disabled={!!loadingId}
          className="flex flex-col gap-0.5 text-left px-2 py-1.5 rounded hover:bg-gray-700 transition-colors disabled:opacity-50 border border-transparent hover:border-gray-600"
        >
          <div className="flex items-center justify-between gap-2">
            <span className="text-gray-400 font-mono text-[10px]">{s.sessionId.slice(0, 8)}…</span>
            <span className="text-gray-600 shrink-0">{timeAgo(s.lastMessageAt)}</span>
          </div>
          <div className="text-gray-300 leading-tight line-clamp-2">{s.lastMessage || '—'}</div>
          <div className="text-gray-600">{s.turnCount} ход{s.turnCount === 1 ? '' : s.turnCount < 5 ? 'а' : 'ов'}</div>
        </button>
      ))}
    </div>
  )
}
