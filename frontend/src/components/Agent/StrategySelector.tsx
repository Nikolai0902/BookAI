import type { ContextStrategyType } from '../../api/agentApi'
import { useAgentStore } from '../../store/useAgentStore'

const STRATEGY_OPTIONS: { value: ContextStrategyType; label: string; description: string }[] = [
  { value: 'FULL_HISTORY',   label: 'Полная история',   description: 'Все сообщения без изменений' },
  { value: 'COMPRESSION',    label: 'Сжатие',            description: 'LLM-саммари + последние N' },
  { value: 'SLIDING_WINDOW', label: 'Скользящее окно',   description: 'Только последние N сообщений' },
  { value: 'STICKY_FACTS',   label: 'Sticky Facts',      description: 'Ключевые факты + последние N' },
  { value: 'BRANCHING',      label: 'Ветвление',          description: 'Чекпоинты и независимые ветки' },
]

export default function StrategySelector() {
  const strategy = useAgentStore((s) => s.strategy)
  const setStrategy = useAgentStore((s) => s.setStrategy)
  const sessionStarted = useAgentStore((s) => s.messages.length > 0)

  const current = STRATEGY_OPTIONS.find((o) => o.value === strategy)

  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-xs text-gray-400 uppercase tracking-wider">Стратегия контекста</label>
      <select
        value={strategy}
        onChange={(e) => setStrategy(e.target.value as ContextStrategyType)}
        disabled={sessionStarted}
        className="bg-gray-800 border border-gray-700 rounded-md px-3 py-2 text-sm text-gray-100 focus:outline-none focus:ring-1 focus:ring-blue-500 cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
      >
        {STRATEGY_OPTIONS.map((o) => (
          <option key={o.value} value={o.value}>{o.label}</option>
        ))}
      </select>
      <p className="text-xs text-gray-600">
        {sessionStarted ? `${current?.label} (зафиксировано)` : current?.description}
      </p>
    </div>
  )
}
