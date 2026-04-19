import type { CompareData } from '../../types/api'

interface Props { answer: string }

export default function CompareView({ answer }: Props) {
  let data: CompareData | null = null
  try {
    data = JSON.parse(answer) as CompareData
  } catch {
    return <div className="text-red-400 text-sm">Не удалось разобрать ответ сравнения</div>
  }

  return (
    <div className="grid grid-cols-2 gap-4 h-full">
      <Column title="Свободный ответ" text={data.freeAnswer} />
      <Column title="Контролируемый (JSON)" text={data.controlledAnswer} />
    </div>
  )
}

function Column({ title, text }: { title: string; text: string }) {
  return (
    <div className="flex flex-col gap-2">
      <div className="text-xs font-medium text-gray-400 uppercase tracking-wider">{title}</div>
      <div className="bg-gray-900 rounded-lg p-4 text-sm text-gray-200 whitespace-pre-wrap leading-relaxed border border-gray-800">
        {text}
      </div>
    </div>
  )
}
