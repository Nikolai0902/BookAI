import { useState } from 'react'
import { useAgentStore } from '../../store/useAgentStore'
import { pauseTask, resumeTask } from '../../api/agentApi'
import type { TaskPhase } from '../../api/agentApi'

const PHASE_LABELS: Record<TaskPhase, string> = {
  NONE:       'None',
  PLANNING:   'Planning',
  EXECUTION:  'Execution',
  VALIDATION: 'Validation',
  DONE:       'Done',
}

const PHASE_COLORS: Record<TaskPhase, string> = {
  NONE:       'text-gray-500',
  PLANNING:   'text-yellow-400',
  EXECUTION:  'text-blue-400',
  VALIDATION: 'text-purple-400',
  DONE:       'text-green-400',
}

export default function TaskStatePanel() {
  const taskState = useAgentStore((s) => s.taskState)
  const sessionId = useAgentStore((s) => s.sessionId)
  const setTaskState = useAgentStore((s) => s.setTaskState)
  const messages = useAgentStore((s) => s.messages)
  const [loading, setLoading] = useState(false)

  if (messages.length === 0 || !taskState || taskState.phase === 'NONE') return null

  const handlePauseResume = async () => {
    if (!sessionId) return
    setLoading(true)
    try {
      if (taskState.paused) {
        await resumeTask(sessionId)
        setTaskState({ ...taskState, paused: false })
      } else {
        await pauseTask(sessionId)
        setTaskState({ ...taskState, paused: true })
      }
    } catch {
      // ignore
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex flex-col gap-2 p-3 bg-gray-800 rounded-lg text-xs">
      <div className="flex items-center justify-between">
        <div className="text-gray-400 uppercase tracking-wider font-medium">Task</div>
        {taskState.paused && (
          <span className="text-yellow-500 text-[10px] uppercase tracking-wider font-bold">Paused</span>
        )}
      </div>

      <div className={`font-semibold ${PHASE_COLORS[taskState.phase]}`}>
        {PHASE_LABELS[taskState.phase]}
      </div>

      {taskState.currentStep && (
        <div>
          <div className="text-gray-500 mb-0.5">Current step</div>
          <span className="text-gray-300">{taskState.currentStep}</span>
        </div>
      )}

      {taskState.expectedAction && !taskState.paused && (
        <div className="border-t border-gray-700 pt-2">
          <div className="text-gray-500 mb-0.5">Expected action</div>
          <span className="text-gray-300">{taskState.expectedAction}</span>
        </div>
      )}

      {taskState.phase === 'PLANNING' && !taskState.planApproved && (
        <div className="border-t border-gray-700 pt-2 flex items-start gap-1.5">
          <span className="text-amber-400">⚠</span>
          <span className="text-amber-300">Ожидает подтверждения плана</span>
        </div>
      )}

      {taskState.blockedTransition && (
        <div className="border-t border-gray-700 pt-2 flex items-start gap-1.5">
          <span className="text-red-400">✗</span>
          <div>
            <div className="text-red-400 font-medium mb-0.5">Переход заблокирован</div>
            <span className="text-red-300">{taskState.blockedTransition}</span>
          </div>
        </div>
      )}

      <button
        onClick={handlePauseResume}
        disabled={loading}
        className={`mt-1 px-2 py-1 rounded text-xs font-medium transition-colors disabled:opacity-50 ${
          taskState.paused
            ? 'bg-blue-700 hover:bg-blue-600 text-white'
            : 'bg-gray-700 hover:bg-gray-600 text-gray-300'
        }`}
      >
        {taskState.paused ? '▶ Resume' : '⏸ Pause'}
      </button>
    </div>
  )
}
