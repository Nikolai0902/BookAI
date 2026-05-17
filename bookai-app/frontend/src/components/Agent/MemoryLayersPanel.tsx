import { useState } from 'react'
import { useAgentStore } from '../../store/useAgentStore'
import { clearLongTermMemory } from '../../api/agentApi'

const CATEGORY_LABELS: Record<string, string> = {
  profile: 'Профиль',
  decision: 'Решения',
  knowledge: 'Знания',
}

const MAX_LINES = 6

function truncateLines(text: string): { visible: string; truncated: boolean } {
  const lines = text.split('\n')
  if (lines.length <= MAX_LINES) return { visible: text, truncated: false }
  return { visible: lines.slice(0, MAX_LINES).join('\n'), truncated: true }
}

export default function MemoryLayersPanel() {
  const snapshot = useAgentStore((s) => s.memoryLayersSnapshot)
  const messages = useAgentStore((s) => s.messages)
  const setMemoryLayersSnapshot = useAgentStore((s) => s.setMemoryLayersSnapshot)
  const profileId = useAgentStore((s) => s.profileId)
  const [clearing, setClearing] = useState(false)

  if (messages.length === 0 || !snapshot) return null

  const hasLongTerm = Object.keys(snapshot.longTermMemory).length > 0

  const handleClearLongTerm = async () => {
    setClearing(true)
    try {
      await clearLongTermMemory(profileId)
      setMemoryLayersSnapshot({ ...snapshot, longTermMemory: {} })
    } finally {
      setClearing(false)
    }
  }

  return (
    <div className="flex flex-col gap-2 p-3 bg-gray-800 rounded-lg text-xs">
      <div className="text-gray-400 uppercase tracking-wider font-medium">Модель памяти</div>

      {/* Краткосрочная */}
      <div>
        <div className="text-gray-500 mb-0.5">Краткосрочная</div>
        <span className="text-blue-300 font-mono">{snapshot.shortTermCount} сообщений в диалоге</span>
      </div>

      {/* Рабочая */}
      <div className="border-t border-gray-700 pt-2">
        <div className="text-gray-500 mb-0.5">Рабочая (сессия)</div>
        {snapshot.workingMemory ? (() => {
          const { visible, truncated } = truncateLines(snapshot.workingMemory)
          return (
            <>
              <pre className="text-gray-300 whitespace-pre-wrap font-mono leading-relaxed">{visible}</pre>
              {truncated && <span className="text-gray-600 italic">…ещё строки скрыты</span>}
            </>
          )
        })() : (
          <span className="text-gray-600 italic">пусто</span>
        )}
      </div>

      {/* Долговременная */}
      <div className="border-t border-gray-700 pt-2">
        <div className="flex items-center justify-between mb-1">
          <div className="text-gray-500">Долговременная (cross-session)</div>
          {hasLongTerm && (
            <button
              onClick={handleClearLongTerm}
              disabled={clearing}
              title="Очистить долговременную память"
              className="text-red-500 hover:text-red-400 disabled:text-gray-600 transition-colors"
            >
              🗑
            </button>
          )}
        </div>
        {hasLongTerm ? (
          <div className="flex flex-col gap-1.5">
            {['profile', 'decision', 'knowledge'].map((cat) =>
              snapshot.longTermMemory[cat] ? (() => {
                const { visible, truncated } = truncateLines(snapshot.longTermMemory[cat])
                return (
                  <div key={cat}>
                    <span className="text-yellow-500 uppercase text-[10px] tracking-wider">
                      {CATEGORY_LABELS[cat] ?? cat}
                    </span>
                    <pre className="text-gray-300 whitespace-pre-wrap font-mono leading-relaxed mt-0.5">{visible}</pre>
                    {truncated && <span className="text-gray-600 italic">…ещё строки скрыты</span>}
                  </div>
                )
              })() : null
            )}
          </div>
        ) : (
          <span className="text-gray-600 italic">пусто</span>
        )}
      </div>
    </div>
  )
}
