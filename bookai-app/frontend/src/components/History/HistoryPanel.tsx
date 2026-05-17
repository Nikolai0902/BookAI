import { useAppStore } from '../../store/useAppStore'
import HistoryItem from './HistoryItem'

export default function HistoryPanel() {
  const history = useAppStore((s) => s.history)

  return (
    <div className="flex flex-col gap-1 min-h-0">
      <div className="text-xs text-gray-400 uppercase tracking-wider">
        История {history.length > 0 && `(${history.length})`}
      </div>
      {history.length === 0 ? (
        <div className="text-xs text-gray-600">Запросов пока нет</div>
      ) : (
        history.map((entry) => <HistoryItem key={entry.id} entry={entry} />)
      )}
    </div>
  )
}
