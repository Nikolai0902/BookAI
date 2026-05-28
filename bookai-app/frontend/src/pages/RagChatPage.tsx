import { useEffect, useRef, useState } from 'react'
import RagChatMessage from '../components/RagChat/RagChatMessage'
import ContextFactsPanel from '../components/RagChat/ContextFactsPanel'
import { useRagChatStore } from '../store/useRagChatStore'
import { sendRagChatMessage, clearRagChatSession } from '../api/ragChatApi'

export default function RagChatPage() {
  const [input, setInput] = useState('')
  const bottomRef = useRef<HTMLDivElement>(null)

  const messages = useRagChatStore((s) => s.messages)
  const sessionId = useRagChatStore((s) => s.sessionId)
  const isLoading = useRagChatStore((s) => s.isLoading)
  const error = useRagChatStore((s) => s.error)
  const contextFacts = useRagChatStore((s) => s.contextFacts)
  const addMessage = useRagChatStore((s) => s.addMessage)
  const setSessionId = useRagChatStore((s) => s.setSessionId)
  const setLoading = useRagChatStore((s) => s.setLoading)
  const setError = useRagChatStore((s) => s.setError)
  const setContextFacts = useRagChatStore((s) => s.setContextFacts)
  const clearSession = useRagChatStore((s) => s.clearSession)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isLoading])

  const handleSend = async () => {
    const text = input.trim()
    if (!text || isLoading) return

    setInput('')
    addMessage({ role: 'user', content: text })
    setLoading(true)
    setError(null)

    try {
      const res = await sendRagChatMessage({
        message: text,
        sessionId: sessionId ?? undefined,
      })
      setSessionId(res.sessionId)
      setContextFacts(res.contextFacts)
      addMessage({
        role: 'assistant',
        content: res.answer,
        citations: res.citations,
        confident: res.confident,
        inputTokens: res.inputTokens,
        outputTokens: res.outputTokens,
        turnNumber: res.turnNumber,
        elapsedMs: res.elapsedMs,
      })
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : 'Ошибка запроса'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  const handleClear = async () => {
    if (sessionId) {
      try {
        await clearRagChatSession(sessionId)
      } catch {
        // ignore — local state is cleared regardless
      }
    }
    clearSession()
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <div className="flex h-screen bg-gray-950 text-gray-100">
      {/* Sidebar */}
      <aside className="w-64 shrink-0 flex flex-col gap-4 p-4 bg-gray-900 border-r border-gray-800 overflow-y-auto">
        <div className="text-base font-semibold text-white tracking-tight">BookAI</div>
        <nav className="flex flex-col gap-1">
          <a href="/" className="flex items-center gap-2 px-3 py-2 rounded-md text-sm text-gray-400 hover:bg-gray-800 hover:text-white transition-colors">
            📖 Book
          </a>
          <a href="/agent" className="flex items-center gap-2 px-3 py-2 rounded-md text-sm text-gray-400 hover:bg-gray-800 hover:text-white transition-colors">
            🤖 Agent
          </a>
          <span className="flex items-center gap-2 px-3 py-2 rounded-md text-sm bg-gray-800 text-white">
            🔍 RAG Chat
          </span>
        </nav>

        <div className="flex flex-col gap-2 mt-2">
          <ContextFactsPanel facts={contextFacts} />
        </div>

        {sessionId && (
          <div className="mt-auto flex flex-col gap-2">
            <div className="text-xs text-gray-600 font-mono break-all">{sessionId}</div>
            <button
              onClick={handleClear}
              className="px-3 py-2 text-xs rounded-md bg-gray-800 text-gray-400 hover:bg-red-900 hover:text-red-200 transition-colors"
            >
              Новый чат
            </button>
          </div>
        )}
      </aside>

      {/* Main */}
      <div className="flex flex-col flex-1 min-w-0">
        {/* Header */}
        <header className="flex items-center gap-3 px-6 py-3 border-b border-gray-800">
          <h1 className="text-sm font-semibold text-white">RAG Chat</h1>
          <span className="text-xs text-gray-500">RAG + история диалога + память задачи</span>
        </header>

        {/* Chat history */}
        <div className="flex-1 overflow-y-auto px-6 py-4">
          {messages.length === 0 && (
            <div className="flex items-center justify-center h-full">
              <p className="text-gray-600 text-sm">Задайте вопрос по документации — ассистент найдёт контекст и ответит с источниками</p>
            </div>
          )}
          {messages.map((msg, i) => (
            <RagChatMessage key={i} {...msg} />
          ))}
          {isLoading && (
            <div className="flex items-start mb-4">
              <div className="bg-gray-800 rounded-xl px-4 py-3">
                <div className="flex gap-1">
                  <span className="w-1.5 h-1.5 bg-gray-500 rounded-full animate-bounce [animation-delay:-0.3s]" />
                  <span className="w-1.5 h-1.5 bg-gray-500 rounded-full animate-bounce [animation-delay:-0.15s]" />
                  <span className="w-1.5 h-1.5 bg-gray-500 rounded-full animate-bounce" />
                </div>
              </div>
            </div>
          )}
          {error && (
            <div className="mb-4 px-4 py-2 bg-red-900/40 border border-red-800 rounded-lg text-sm text-red-300">
              {error}
            </div>
          )}
          <div ref={bottomRef} />
        </div>

        {/* Input */}
        <div className="px-6 py-4 border-t border-gray-800">
          <div className="flex gap-3 items-end">
            <textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Введите вопрос... (Enter — отправить, Shift+Enter — новая строка)"
              rows={2}
              disabled={isLoading}
              className="flex-1 resize-none bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-sm text-gray-100 placeholder-gray-600 focus:outline-none focus:border-blue-600 disabled:opacity-50"
            />
            <button
              onClick={handleSend}
              disabled={isLoading || !input.trim()}
              className="px-5 py-3 bg-blue-600 hover:bg-blue-500 disabled:opacity-40 disabled:cursor-not-allowed text-white text-sm font-medium rounded-lg transition-colors"
            >
              Отправить
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
