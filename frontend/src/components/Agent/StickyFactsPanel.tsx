import { useAgentStore } from '../../store/useAgentStore'

export default function StickyFactsPanel() {
  const strategy = useAgentStore((s) => s.strategy)
  const factsSnapshot = useAgentStore((s) => s.factsSnapshot)
  const messages = useAgentStore((s) => s.messages)

  if (strategy !== 'STICKY_FACTS' || messages.length === 0) return null

  return (
    <div className="flex flex-col gap-1.5 p-3 bg-gray-800 rounded-lg text-xs">
      <div className="text-gray-400 uppercase tracking-wider font-medium">Факты сессии</div>
      <pre className="text-gray-300 whitespace-pre-wrap font-mono text-xs leading-relaxed">
        {factsSnapshot ?? '—'}
      </pre>
    </div>
  )
}
