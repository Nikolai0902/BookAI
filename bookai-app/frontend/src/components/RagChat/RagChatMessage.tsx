import { useState } from 'react'
import ReactMarkdown from 'react-markdown'
import rehypeHighlight from 'rehype-highlight'
import type { RagChatMessage as RagChatMessageType } from '../../store/useRagChatStore'

type Props = RagChatMessageType

export default function RagChatMessage({
  role, content, citations, confident, inputTokens, outputTokens, turnNumber, elapsedMs,
}: Props) {
  const [showCitations, setShowCitations] = useState(false)
  const isUser = role === 'user'

  return (
    <div className={`flex flex-col ${isUser ? 'items-end' : 'items-start'} mb-4`}>
      <div
        className={`max-w-[75%] rounded-xl px-4 py-3 text-sm ${
          isUser ? 'bg-blue-600 text-white' : 'bg-gray-800 text-gray-100'
        }`}
      >
        {isUser ? (
          <p className="whitespace-pre-wrap">{content}</p>
        ) : (
          <div className="prose prose-invert prose-sm max-w-none">
            <ReactMarkdown rehypePlugins={[rehypeHighlight]}>{content}</ReactMarkdown>
          </div>
        )}
      </div>

      {!isUser && (
        <div className="flex flex-wrap items-center gap-3 mt-1 px-1 text-xs text-gray-600">
          {turnNumber !== undefined && <span title="Ход">#{turnNumber}</span>}
          {inputTokens !== undefined && <span title="Токены запроса">↑ {inputTokens}</span>}
          {outputTokens !== undefined && <span title="Токены ответа">↓ {outputTokens}</span>}
          {elapsedMs !== undefined && <span title="Время ответа">{elapsedMs} мс</span>}
          {confident === false && (
            <span className="text-yellow-600 font-medium">⚠ нет контекста</span>
          )}
          {citations && citations.length > 0 && (
            <button
              onClick={() => setShowCitations((v) => !v)}
              className="text-blue-500 hover:text-blue-400 transition-colors"
            >
              {showCitations ? '▲' : '▼'} {citations.length} источн.
            </button>
          )}
        </div>
      )}

      {!isUser && showCitations && citations && citations.length > 0 && (
        <div className="mt-2 max-w-[75%] flex flex-col gap-2">
          {citations.map((c) => (
            <div key={c.rank} className="rounded-lg bg-gray-900 border border-gray-700 px-3 py-2 text-xs">
              <div className="flex items-center gap-2 mb-1">
                <span className="text-blue-400 font-mono font-bold">[{c.rank}]</span>
                <span className="text-gray-400 truncate">{c.source}</span>
                <span className="text-gray-600">|</span>
                <span className="text-gray-500 truncate">{c.section}</span>
                <span className="ml-auto text-green-600 font-mono">{(c.score * 100).toFixed(0)}%</span>
              </div>
              <p className="text-gray-400 italic leading-relaxed">«{c.snippet}»</p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
