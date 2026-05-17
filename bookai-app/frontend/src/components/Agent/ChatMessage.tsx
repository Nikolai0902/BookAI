import ReactMarkdown from 'react-markdown'
import rehypeHighlight from 'rehype-highlight'
import type { ChatMessage as ChatMessageType } from '../../store/useAgentStore'

type Props = ChatMessageType

export default function ChatMessage({ role, content, inputTokens, outputTokens, turnNumber, responseTimeMs }: Props) {
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

      {!isUser && inputTokens !== undefined && (
        <div className="flex gap-3 mt-1 px-1 text-xs text-gray-600">
          <span title="Ход">#{turnNumber}</span>
          <span title="Токены запроса">↑ {inputTokens}</span>
          <span title="Токены ответа">↓ {outputTokens}</span>
          {responseTimeMs !== undefined && (
            <span title="Время ответа">{responseTimeMs} мс</span>
          )}
        </div>
      )}
    </div>
  )
}
