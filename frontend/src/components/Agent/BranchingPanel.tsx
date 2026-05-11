import { useState } from 'react'
import { useAgentStore } from '../../store/useAgentStore'
import { createBranch } from '../../api/agentApi'
import type { BranchInfo } from '../../api/agentApi'

export default function BranchingPanel() {
  const strategy = useAgentStore((s) => s.strategy)
  const sessionId = useAgentStore((s) => s.sessionId)
  const messages = useAgentStore((s) => s.messages)
  const branches = useAgentStore((s) => s.branches)
  const addBranch = useAgentStore((s) => s.addBranch)
  const switchToBranch = useAgentStore((s) => s.switchToBranch)

  const [label, setLabel] = useState('')
  const [creating, setCreating] = useState(false)
  const [showInput, setShowInput] = useState(false)

  if (strategy !== 'BRANCHING') return null

  const assistantMessages = messages.filter((m) => m.role === 'assistant')
  const lastMessageId = assistantMessages.length > 0
    ? assistantMessages[assistantMessages.length - 1].lastMessageId
    : null

  const handleCreate = async () => {
    if (!sessionId || lastMessageId == null) return
    setCreating(true)
    try {
      const branch = await createBranch({
        rootSessionId: sessionId,
        checkpointMessageId: lastMessageId,
        label: label.trim() || `Ветка ${branches.length + 1}`,
      })
      addBranch(branch)
      setLabel('')
      setShowInput(false)
    } catch {
      // ignore
    } finally {
      setCreating(false)
    }
  }

  const handleSwitch = (branch: BranchInfo) => {
    switchToBranch(branch.branchSessionId)
  }

  return (
    <div className="flex flex-col gap-2 p-3 bg-gray-800 rounded-lg text-xs">
      <div className="text-gray-400 uppercase tracking-wider font-medium">Ветки диалога</div>

      {sessionId && messages.length > 0 && (
        <div className="flex flex-col gap-1">
          {!showInput ? (
            <button
              onClick={() => setShowInput(true)}
              className="text-left text-blue-400 hover:text-blue-300 transition-colors"
            >
              + Создать ветку здесь
            </button>
          ) : (
            <div className="flex gap-1">
              <input
                type="text"
                value={label}
                onChange={(e) => setLabel(e.target.value)}
                placeholder="Название ветки"
                className="flex-1 bg-gray-700 border border-gray-600 rounded px-2 py-1 text-xs text-gray-100 focus:outline-none focus:ring-1 focus:ring-blue-500"
                onKeyDown={(e) => e.key === 'Enter' && handleCreate()}
              />
              <button
                onClick={handleCreate}
                disabled={creating}
                className="px-2 py-1 bg-blue-600 hover:bg-blue-500 rounded text-white disabled:opacity-50 transition-colors"
              >
                ОК
              </button>
              <button
                onClick={() => { setShowInput(false); setLabel('') }}
                className="px-2 py-1 text-gray-400 hover:text-white transition-colors"
              >
                ✕
              </button>
            </div>
          )}
        </div>
      )}

      {branches.length > 0 && (
        <div className="flex flex-col gap-1 mt-1 border-t border-gray-700 pt-2">
          {branches.map((b) => (
            <div key={b.branchSessionId} className="flex items-center justify-between gap-2">
              <span className="text-gray-300 truncate" title={b.branchSessionId}>{b.label}</span>
              <button
                onClick={() => handleSwitch(b)}
                disabled={sessionId === b.branchSessionId}
                className="shrink-0 px-2 py-0.5 text-xs bg-gray-700 hover:bg-gray-600 rounded text-gray-300 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
              >
                {sessionId === b.branchSessionId ? 'активна' : 'войти'}
              </button>
            </div>
          ))}
        </div>
      )}

      {branches.length === 0 && messages.length === 0 && (
        <p className="text-gray-600">Начните диалог, затем создайте ветку</p>
      )}
    </div>
  )
}
