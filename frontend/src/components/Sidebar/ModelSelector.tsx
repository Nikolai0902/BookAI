import { useAppStore } from '../../store/useAppStore'

const OPTIONS = [
  { value: '',                         label: 'По умолчанию' },
  { value: 'claude-haiku-4-5-20251001', label: 'Haiku (слабая)' },
  { value: 'claude-sonnet-4-6',         label: 'Sonnet (средняя)' },
  { value: 'claude-opus-4-7',           label: 'Opus (сильная)' },
]

export default function ModelSelector() {
  const { model, setModel } = useAppStore()

  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-xs text-gray-400 uppercase tracking-wider">Выбор модели</label>
      <select
        value={model ?? ''}
        onChange={(e) => setModel(e.target.value || null)}
        className="bg-gray-800 border border-gray-700 rounded-md px-3 py-2 text-sm text-gray-100 focus:outline-none focus:ring-1 focus:ring-blue-500 cursor-pointer"
      >
        {OPTIONS.map((o) => (
          <option key={o.value} value={o.value}>{o.label}</option>
        ))}
      </select>
    </div>
  )
}
