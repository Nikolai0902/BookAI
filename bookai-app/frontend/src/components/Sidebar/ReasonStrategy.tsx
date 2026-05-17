import { useAppStore, type Strategy } from '../../store/useAppStore'

const OPTIONS: { value: Strategy; label: string }[] = [
  { value: 'DIRECT', label: 'Direct' },
  { value: 'STEP_BY_STEP', label: 'Step-by-step' },
  { value: 'META_PROMPT', label: 'Meta-prompt' },
  { value: 'EXPERT_PANEL', label: 'Expert panel' },
]

export default function ReasonStrategy() {
  const { strategy, setStrategy } = useAppStore()

  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-xs text-gray-400 uppercase tracking-wider">Стратегия</label>
      <select
        value={strategy}
        onChange={(e) => setStrategy(e.target.value as Strategy)}
        className="bg-gray-800 border border-gray-700 rounded-md px-3 py-2 text-sm text-gray-100 focus:outline-none focus:ring-1 focus:ring-blue-500 cursor-pointer"
      >
        {OPTIONS.map((o) => (
          <option key={o.value} value={o.value}>{o.label}</option>
        ))}
      </select>
    </div>
  )
}
