import { useAppStore, type Mode } from '../../store/useAppStore'

const OPTIONS: { value: Mode; label: string }[] = [
  { value: 'ASK', label: 'Ask' },
  { value: 'COMPARE', label: 'Compare' },
  { value: 'REASON', label: 'Reason' },
]

export default function FilterSelector() {
  const { mode, setMode } = useAppStore()

  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-xs text-gray-400 uppercase tracking-wider">Режим</label>
      <select
        value={mode}
        onChange={(e) => setMode(e.target.value as Mode)}
        className="bg-gray-800 border border-gray-700 rounded-md px-3 py-2 text-sm text-gray-100 focus:outline-none focus:ring-1 focus:ring-blue-500 cursor-pointer"
      >
        {OPTIONS.map((o) => (
          <option key={o.value} value={o.value}>{o.label}</option>
        ))}
      </select>
    </div>
  )
}
