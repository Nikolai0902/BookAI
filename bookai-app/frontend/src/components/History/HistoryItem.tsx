import { useAppStore, type HistoryEntry } from '../../store/useAppStore'

const MODE_BADGE: Record<string, string> = {
  ASK: 'Ask',
  COMPARE: 'Cmp',
  REASON: 'Rsn',
}

interface Props { entry: HistoryEntry }

export default function HistoryItem({ entry }: Props) {
  const restoreFromHistory = useAppStore((s) => s.restoreFromHistory)

  return (
    <button
      onClick={() => restoreFromHistory(entry)}
      className="flex items-start gap-2 w-full text-left px-2 py-1.5 rounded hover:bg-gray-800 transition-colors group"
    >
      <span className="shrink-0 text-xs text-blue-400 font-mono mt-0.5 w-7">
        {MODE_BADGE[entry.mode]}
      </span>
      <span className="text-xs text-gray-400 group-hover:text-gray-200 line-clamp-2 transition-colors">
        {entry.prompt}
      </span>
    </button>
  )
}
