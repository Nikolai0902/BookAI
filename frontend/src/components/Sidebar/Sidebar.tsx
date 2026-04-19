import { useAppStore } from '../../store/useAppStore'
import FilterSelector from './FilterSelector'
import ReasonStrategy from './ReasonStrategy'
import TemperatureInput from './TemperatureInput'
import ModelInfo from './ModelInfo'
import ModelSelector from './ModelSelector'
import StatsPanel from './StatsPanel'
import HistoryPanel from '../History/HistoryPanel'

export default function Sidebar() {
  const mode = useAppStore((s) => s.mode)

  return (
    <aside className="w-64 shrink-0 flex flex-col gap-4 p-4 bg-gray-900 border-r border-gray-800 overflow-y-auto">
      <div className="text-base font-semibold text-white tracking-tight">BookAI</div>
      <FilterSelector />
      {mode === 'REASON' && <ReasonStrategy />}
      <ModelSelector />
      <TemperatureInput />
      <ModelInfo />
      <StatsPanel />
      <hr className="border-gray-800" />
      <HistoryPanel />
    </aside>
  )
}
