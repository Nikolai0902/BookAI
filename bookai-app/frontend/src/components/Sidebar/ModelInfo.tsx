import { useEffect, useState } from 'react'
import axios from 'axios'
import { useAgentStore } from '../../store/useAgentStore'

interface InfoResponse {
  model: string
  maxTokens: number
}

export default function ModelInfo() {
  const [info, setInfo] = useState<InfoResponse | null>(null)
  const messages = useAgentStore((s) => s.messages)

  useEffect(() => {
    axios.get<InfoResponse>('/api/info')
      .then((r) => setInfo(r.data))
      .catch(() => setInfo({ model: 'unavailable', maxTokens: 0 }))
  }, [])

  const lastAssistant = [...messages].reverse().find((m) => m.role === 'assistant')
  const lastOutputTokens = lastAssistant?.outputTokens ?? 0
  const maxTokens = info?.maxTokens ?? 0
  const fillPct = maxTokens > 0 ? Math.min(100, (lastOutputTokens / maxTokens) * 100) : 0
  const isWarning = fillPct >= 80
  const isDanger = fillPct >= 100

  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-xs text-gray-400 uppercase tracking-wider">Модель по умолчанию</label>
      <div className="bg-gray-800 border border-gray-700 rounded-md px-3 py-2 text-sm text-blue-300 font-mono truncate">
        {info?.model ?? '…'}
      </div>

      {maxTokens > 0 && (
        <div className="flex flex-col gap-1">
          <div className="flex justify-between text-xs text-gray-500">
            <span>Последний ответ</span>
            <span className={isDanger ? 'text-red-400' : isWarning ? 'text-yellow-400' : 'text-gray-400'}>
              {lastOutputTokens.toLocaleString()} / {maxTokens.toLocaleString()}
            </span>
          </div>
          <div className="w-full h-1.5 bg-gray-700 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full transition-all duration-300 ${
                isDanger ? 'bg-red-500' : isWarning ? 'bg-yellow-400' : 'bg-blue-500'
              }`}
              style={{ width: `${fillPct}%` }}
            />
          </div>
          {isDanger && (
            <p className="text-xs text-red-400">Ответ обрезан — модель достигла лимита</p>
          )}
        </div>
      )}
    </div>
  )
}
