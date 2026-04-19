import { useState } from 'react'
import { Link } from 'react-router-dom'
import ChatHistory from '../components/Agent/ChatHistory'
import { useAgentStore } from '../store/useAgentStore'
import { sendAgentMessage } from '../api/agentApi'

const MODEL_OPTIONS = [
  { value: '',                          label: 'По умолчанию' },
  { value: 'claude-haiku-4-5-20251001', label: 'Haiku (быстрая)' },
  { value: 'claude-sonnet-4-6',         label: 'Sonnet (средняя)' },
  { value: 'claude-opus-4-7',           label: 'Opus (сильная)' },
]

export default function AgentPage() {
  const [input, setInput] = useState('')
  const { sessionId, model, isLoading, error, addMessage, setSessionId, setLoading, setError, setModel, clearSession } =
    useAgentStore()

  const handleSend = async () => {
    const text = input.trim()
    if (!text || isLoading) return

    addMessage({ role: 'user', content: text })
    setInput('')
    setLoading(true)
    setError(null)

    try {
      const res = await sendAgentMessage({
        message: text,
        sessionId: sessionId ?? undefined,
        model: model || null,
      })
      setSessionId(res.sessionId)
      addMessage({ role: 'assistant', content: res.reply })
    } catch {
      setError('Ошибка при обращении к агенту')
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

  return (
    <div className="flex h-screen bg-gray-950 text-gray-100">
      <aside className="w-64 shrink-0 flex flex-col gap-4 p-4 bg-gray-900 border-r border-gray-800">
        <div className="text-base font-semibold text-white tracking-tight">BookAI</div>

        <nav className="flex flex-col gap-1">
          <Link
            to="/"
            className="flex items-center gap-2 px-3 py-2 rounded-md text-sm text-gray-400 hover:bg-gray-800 hover:text-white transition-colors"
          >
            📖 Book
          </Link>
          <Link
            to="/agent"
            className="flex items-center gap-2 px-3 py-2 rounded-md text-sm bg-gray-800 text-white"
          >
            🤖 Agent
          </Link>
        </nav>

        <div className="flex flex-col gap-1.5">
          <label className="text-xs text-gray-400 uppercase tracking-wider">Выбор модели</label>
          <select
            value={model ?? ''}
            onChange={(e) => setModel(e.target.value || null)}
            className="bg-gray-800 border border-gray-700 rounded-md px-3 py-2 text-sm text-gray-100 focus:outline-none focus:ring-1 focus:ring-blue-500 cursor-pointer"
          >
            {MODEL_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>{o.label}</option>
            ))}
          </select>
        </div>

        <button
          onClick={clearSession}
          className="px-3 py-2 text-sm text-gray-400 hover:text-white hover:bg-gray-800 rounded-md transition-colors text-left"
        >
          🗑 Новый разговор
        </button>
      </aside>

      <main className="flex flex-col flex-1 overflow-hidden">
        <ChatHistory />
        {error && (
          <div className="px-6 py-2 text-sm text-red-400">{error}</div>
        )}
        <div className="p-4 border-t border-gray-800">
          <div className="flex gap-2">
            <textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Напишите сообщение... (Enter для отправки)"
              rows={1}
              className="flex-1 bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-sm text-gray-100 placeholder-gray-500 focus:outline-none focus:ring-1 focus:ring-blue-500 resize-none"
              style={{ maxHeight: '200px', overflowY: 'auto' }}
            />
            <button
              onClick={handleSend}
              disabled={!input.trim() || isLoading}
              className="px-4 py-2 bg-blue-600 hover:bg-blue-500 disabled:bg-gray-700 disabled:text-gray-500 text-white rounded-lg text-sm font-medium transition-colors"
            >
              →
            </button>
          </div>
        </div>
      </main>
    </div>
  )
}
