import { Link, useLocation } from 'react-router-dom'
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
  const location = useLocation()

  return (
    <aside className="w-64 shrink-0 flex flex-col gap-4 p-4 bg-gray-900 border-r border-gray-800 overflow-y-auto">
      <div className="text-base font-semibold text-white tracking-tight">BookAI</div>
      <nav className="flex flex-col gap-1">
        <Link
          to="/"
          className={`flex items-center gap-2 px-3 py-2 rounded-md text-sm transition-colors ${
            location.pathname === '/' ? 'bg-gray-800 text-white' : 'text-gray-400 hover:bg-gray-800 hover:text-white'
          }`}
        >
          📖 Book
        </Link>
        <Link
          to="/agent"
          className={`flex items-center gap-2 px-3 py-2 rounded-md text-sm transition-colors ${
            location.pathname === '/agent' ? 'bg-gray-800 text-white' : 'text-gray-400 hover:bg-gray-800 hover:text-white'
          }`}
        >
          🤖 Agent
        </Link>
      </nav>
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
