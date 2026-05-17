interface Props { answer: string }

export default function AskView({ answer }: Props) {
  return (
    <div className="text-gray-200 text-sm leading-relaxed whitespace-pre-wrap">
      {answer}
    </div>
  )
}
