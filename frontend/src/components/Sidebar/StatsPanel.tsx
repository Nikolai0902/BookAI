import { useAppStore } from '../../store/useAppStore'

const MODE_LABELS: Record<string, string> = {
  ASK: 'Ask',
  COMPARE: 'Compare',
  REASON: 'Reason',
}

export default function StatsPanel() {
  const history = useAppStore((s) => s.history)
  const inputTokens = useAppStore((s) => s.inputTokens)
  const outputTokens = useAppStore((s) => s.outputTokens)

  const total = history.length
  const lastMode = history[0]?.mode ?? null
  const lastDate = history[0]
    ? new Date(history[0].timestamp).toLocaleString('ru-RU', {
        day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit',
      })
    : null

  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-xs text-gray-400 uppercase tracking-wider">Статистика</label>
      <div className="bg-gray-800 border border-gray-700 rounded-md px-3 py-2 flex flex-col gap-1 text-sm text-gray-300">
        <div className="flex justify-between">
          <span className="text-gray-500">Запросов</span>
          <span className="font-semibold text-white">{total}</span>
        </div>
        {lastMode && (
          <div className="flex justify-between">
            <span className="text-gray-500">Последний режим</span>
            <span className="text-blue-300">{MODE_LABELS[lastMode]}</span>
          </div>
        )}
        {lastDate && (
          <div className="flex justify-between">
            <span className="text-gray-500">Время</span>
            <span className="text-gray-400 text-xs">{lastDate}</span>
          </div>
        )}
        {inputTokens !== null && (
          <div className="flex justify-between pt-1 border-t border-gray-700">
            <span className="text-gray-500">Входящих токенов</span>
            <span className="text-green-400 font-mono text-xs">{inputTokens.toLocaleString()}</span>
          </div>
        )}
        {outputTokens !== null && (
          <div className="flex justify-between">
            <span className="text-gray-500">Исходящих токенов</span>
            <span className="text-yellow-400 font-mono text-xs">{outputTokens.toLocaleString()}</span>
          </div>
        )}
        {total === 0 && (
          <span className="text-gray-600 text-xs">Нет истории</span>
        )}
      </div>
    </div>
  )
}
