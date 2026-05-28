const FACT_LABELS: Record<string, string> = {
  goal: 'Цель',
  clarifications: 'Уточнения',
  constraints: 'Ограничения',
  terms: 'Термины',
}

interface Props {
  facts: string | null
}

export default function ContextFactsPanel({ facts }: Props) {
  if (!facts || facts.trim() === '') return null

  const parsed: Record<string, string> = {}
  facts.split('\n').forEach((line) => {
    const idx = line.indexOf(':')
    if (idx === -1) return
    const key = line.slice(0, idx).trim()
    const value = line.slice(idx + 1).trim()
    if (key && value) parsed[key] = value
  })

  const entries = Object.entries(parsed)
  if (entries.length === 0) return null

  return (
    <div className="flex flex-col gap-2 p-3 bg-gray-800 rounded-lg text-xs border border-gray-700">
      <div className="text-gray-400 uppercase tracking-wider font-medium">Память задачи</div>
      {entries.map(([key, value]) => (
        <div key={key} className="flex flex-col gap-0.5">
          <span className="text-purple-400 font-medium">
            {FACT_LABELS[key] ?? key}
          </span>
          <span className="text-gray-300 leading-relaxed">{value}</span>
        </div>
      ))}
    </div>
  )
}
