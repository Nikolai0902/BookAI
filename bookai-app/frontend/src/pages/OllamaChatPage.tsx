import { useEffect, useRef, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useOllamaChatStore } from '../store/useOllamaChatStore'
import { sendOllamaMessage, checkOllamaStatus } from '../api/ollamaChatApi'

export default function OllamaChatPage() {
  const [input, setInput] = useState('')
  const bottomRef = useRef<HTMLDivElement>(null)
  const location = useLocation()

  const messages = useOllamaChatStore((s) => s.messages)
  const isLoading = useOllamaChatStore((s) => s.isLoading)
  const error = useOllamaChatStore((s) => s.error)
  const modelInfo = useOllamaChatStore((s) => s.modelInfo)
  const addMessage = useOllamaChatStore((s) => s.addMessage)
  const setLoading = useOllamaChatStore((s) => s.setLoading)
  const setError = useOllamaChatStore((s) => s.setError)
  const setModelInfo = useOllamaChatStore((s) => s.setModelInfo)
  const clearMessages = useOllamaChatStore((s) => s.clearMessages)

  useEffect(() => {
    checkOllamaStatus()
      .then(setModelInfo)
      .catch(() => setModelInfo({ available: false, model: 'qwen2.5:3b' }))
  }, [setModelInfo])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isLoading])

  const buildHistory = () =>
    messages.map((m) => ({ role: m.role, content: m.content }))

  const handleSend = async () => {
    const text = input.trim()
    if (!text || isLoading) return

    setInput('')
    addMessage({ role: 'user', content: text })
    setLoading(true)
    setError(null)

    try {
      const res = await sendOllamaMessage({ message: text, history: buildHistory() })
      addMessage({
        role: 'assistant',
        content: res.answer,
        elapsedMs: res.elapsedMs,
        evalCount: res.evalCount,
        model: res.model,
      })
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : 'Ошибка запроса'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const navLinks = [
    { to: '/', label: '📖 Book' },
    { to: '/agent', label: '🤖 Agent' },
    { to: '/rag-chat', label: '🔍 RAG Chat' },
    { to: '/ollama-chat', label: '🦙 Local LLM' },
  ]

  return (
    <div className="flex h-screen bg-gray-950 text-gray-100">
      {/* Sidebar */}
      <aside className="w-64 shrink-0 flex flex-col gap-4 p-4 bg-gray-900 border-r border-gray-800 overflow-y-auto">
        <div className="text-base font-semibold text-white tracking-tight">BookAI</div>
        <nav className="flex flex-col gap-1">
          {navLinks.map(({ to, label }) => (
            <Link
              key={to}
              to={to}
              className={`flex items-center gap-2 px-3 py-2 rounded-md text-sm transition-colors ${
                location.pathname === to
                  ? 'bg-gray-800 text-white'
                  : 'text-gray-400 hover:bg-gray-800 hover:text-white'
              }`}
            >
              {label}
            </Link>
          ))}
        </nav>

        {/* Ollama status */}
        <div className="flex flex-col gap-2 mt-2 p-3 bg-gray-800 rounded-lg border border-gray-700 text-xs">
          <div className="text-gray-400 uppercase tracking-wider font-medium">Ollama</div>
          {modelInfo ? (
            <div className="flex items-center gap-2">
              <span className={`w-2 h-2 rounded-full flex-shrink-0 ${modelInfo.available ? 'bg-green-400' : 'bg-red-500'}`} />
              <span className={modelInfo.available ? 'text-green-300' : 'text-red-400'}>
                {modelInfo.available ? modelInfo.model : 'Недоступна'}
              </span>
            </div>
          ) : (
            <span className="text-gray-500">Проверка...</span>
          )}
        </div>

        {messages.length > 0 && (
          <div className="mt-auto">
            <button
              onClick={clearMessages}
              className="w-full px-3 py-2 text-xs rounded-md bg-gray-800 text-gray-400 hover:bg-red-900 hover:text-red-200 transition-colors"
            >
              Очистить чат
            </button>
          </div>
        )}
      </aside>

      {/* Main */}
      <div className="flex flex-col flex-1 min-w-0">
        {/* Header */}
        <header className="flex items-center gap-3 px-6 py-3 border-b border-gray-800">
          <h1 className="text-sm font-semibold text-white">Local LLM</h1>
          <span className="px-2 py-0.5 text-xs rounded-full bg-purple-900 text-purple-300 font-medium">Local</span>
          <span className="text-xs text-gray-500">Запросы идут только на localhost — без облака</span>
        </header>

        {/* Chat history */}
        <div className="flex-1 overflow-y-auto px-6 py-4">
          {messages.length === 0 && (
            <div className="flex items-center justify-center h-full">
              <p className="text-gray-600 text-sm">Начните диалог — ответы генерирует локальная модель Ollama</p>
            </div>
          )}
          {messages.map((msg, i) => (
            <div
              key={i}
              className={`flex mb-4 ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
            >
              <div
                className={`max-w-2xl rounded-xl px-4 py-3 text-sm leading-relaxed ${
                  msg.role === 'user'
                    ? 'bg-blue-700 text-white'
                    : 'bg-gray-800 text-gray-100'
                }`}
              >
                <div className="whitespace-pre-wrap">{msg.content}</div>
                {msg.role === 'assistant' && (msg.elapsedMs !== undefined || msg.evalCount !== undefined) && (
                  <div className="flex gap-3 mt-2 text-xs text-gray-500">
                    {msg.evalCount !== undefined && <span>{msg.evalCount} токенов</span>}
                    {msg.elapsedMs !== undefined && <span>{msg.elapsedMs} мс</span>}
                    {msg.model && <span className="text-purple-500">{msg.model}</span>}
                  </div>
                )}
              </div>
            </div>
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
              className="flex-1 resize-none bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-sm text-gray-100 placeholder-gray-600 focus:outline-none focus:border-purple-600 disabled:opacity-50"
            />
            <button
              onClick={handleSend}
              disabled={isLoading || !input.trim()}
              className="px-5 py-3 bg-purple-700 hover:bg-purple-600 disabled:opacity-40 disabled:cursor-not-allowed text-white text-sm font-medium rounded-lg transition-colors"
            >
              Отправить
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
