import { useState } from 'react'
import { Link } from 'react-router-dom'
import ChatHistory from '../components/Agent/ChatHistory'
import ModelInfo from '../components/Sidebar/ModelInfo'
import { useAgentStore } from '../store/useAgentStore'
import { sendAgentMessage } from '../api/agentApi'

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false)
  const handleCopy = () => {
    navigator.clipboard.writeText(text)
    setCopied(true)
    setTimeout(() => setCopied(false), 1500)
  }
  return (
    <button
      onClick={handleCopy}
      title="Скопировать Session ID"
      className="px-2 py-1 text-xs text-gray-400 hover:text-white hover:bg-gray-700 rounded transition-colors shrink-0"
    >
      {copied ? '✓' : '⎘'}
    </button>
  )
}

const MODEL_OPTIONS = [
  { value: '',                          label: 'По умолчанию' },
  { value: 'claude-haiku-4-5-20251001', label: 'Haiku (быстрая)' },
  { value: 'claude-sonnet-4-6',         label: 'Sonnet (средняя)' },
  { value: 'claude-opus-4-7',           label: 'Opus (сильная)' },
]

// Цены Anthropic на апрель 2026, $ за 1M токенов
const MODEL_PRICING: Record<string, { input: number; output: number }> = {
  'claude-haiku-4-5-20251001': { input: 0.80, output: 4 },
  'claude-sonnet-4-6':         { input: 3,    output: 15 },
  'claude-opus-4-7':           { input: 15,   output: 75 },
}
const DEFAULT_PRICING = MODEL_PRICING['claude-sonnet-4-6']

function TokenStatsPanel() {
  const tokenTotals = useAgentStore((s) => s.tokenTotals)
  const messages = useAgentStore((s) => s.messages)
  const model = useAgentStore((s) => s.model)

  const assistantMessages = messages.filter((m) => m.role === 'assistant' && m.inputTokens !== undefined)

  if (!tokenTotals && assistantMessages.length === 0) return null

  const last = assistantMessages[assistantMessages.length - 1]
  const prev = assistantMessages[assistantMessages.length - 2]

  // inputTokens последнего запроса = вся история, которую видела модель
  const historyTokens = last?.inputTokens ?? 0
  // разница между последними двумя запросами = сколько добавило новое сообщение + ответ
  const newMessageTokens = prev ? historyTokens - (prev.inputTokens ?? 0) : historyTokens

  const totalIn = tokenTotals?.totalInputTokens ?? 0
  const totalOut = tokenTotals?.totalOutputTokens ?? 0
  const turns = tokenTotals?.turnNumber ?? 0

  const pricing = (model && MODEL_PRICING[model]) ? MODEL_PRICING[model] : DEFAULT_PRICING
  const sessionCost = ((totalIn * pricing.input + totalOut * pricing.output) / 1_000_000).toFixed(6)

  return (
    <div className="flex flex-col gap-2 p-3 bg-gray-800 rounded-lg text-xs">
      <div className="text-gray-400 uppercase tracking-wider font-medium">Токены сессии</div>

      <div className="grid grid-cols-2 gap-x-4 gap-y-1.5">
        <span className="text-gray-500">Ходов</span>
        <span className="text-gray-200 text-right font-mono">{turns}</span>

        <span className="text-gray-400 col-span-2 pt-1 border-t border-gray-700">Текущий запрос</span>

        <span className="text-gray-500" title="Сколько токенов добавило последнее сообщение пользователя + предыдущий ответ">Новое сообщение ≈</span>
        <span className="text-blue-300 text-right font-mono">{newMessageTokens.toLocaleString()}</span>

        <span className="text-gray-500" title="inputTokens последнего запроса = вся история, которую сейчас видит модель">История (весь контекст)</span>
        <span className="text-blue-400 text-right font-mono">{historyTokens.toLocaleString()}</span>

        <span className="text-gray-500" title="outputTokens последнего ответа">Ответ модели</span>
        <span className="text-green-400 text-right font-mono">{(last?.outputTokens ?? 0).toLocaleString()}</span>

        <span className="text-gray-400 col-span-2 pt-1 border-t border-gray-700">За всю сессию (стоимость)</span>

        <span className="text-gray-500" title="Сумма всех input-токенов — именно столько реально оплачено">Оплачено input</span>
        <span className="text-blue-400 text-right font-mono">{totalIn.toLocaleString()}</span>

        <span className="text-gray-500">Оплачено output</span>
        <span className="text-green-400 text-right font-mono">{totalOut.toLocaleString()}</span>

        <span className="text-gray-500" title={`$${pricing.input}/1M input · $${pricing.output}/1M output`}>Стоимость сессии $</span>
        <span className="text-yellow-400 text-right font-mono">{sessionCost}</span>
      </div>

      {assistantMessages.length > 1 && (
        <div className="mt-1 border-t border-gray-700 pt-2">
          <div className="text-gray-500 mb-1">Рост контекста по ходам:</div>
          <div className="flex items-end gap-0.5 h-8">
            {assistantMessages.map((m, i) => {
              const maxIn = Math.max(...assistantMessages.map((a) => a.inputTokens ?? 0))
              const height = maxIn > 0 ? Math.max(4, ((m.inputTokens ?? 0) / maxIn) * 28) : 4
              return (
                <div
                  key={i}
                  title={`Ход ${i + 1}: история ${m.inputTokens} / ответ ${m.outputTokens}`}
                  className="flex-1 bg-blue-500 rounded-sm opacity-80 hover:opacity-100 transition-opacity cursor-default"
                  style={{ height: `${height}px` }}
                />
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}

export default function AgentPage() {
  const [input, setInput] = useState('')
  const { sessionId, model, isLoading, error, addMessage, setSessionId, setLoading, setError, setModel, clearSession, setTokenTotals } =
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
      addMessage({
        role: 'assistant',
        content: res.reply,
        inputTokens: res.inputTokens,
        outputTokens: res.outputTokens,
        totalInputTokens: res.totalInputTokens,
        totalOutputTokens: res.totalOutputTokens,
        turnNumber: res.turnNumber,
        responseTimeMs: res.responseTimeMs,
      })
      setTokenTotals({
        totalInputTokens: res.totalInputTokens,
        totalOutputTokens: res.totalOutputTokens,
        turnNumber: res.turnNumber,
      })
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg ? `Ошибка: ${msg}` : 'Ошибка при обращении к агенту')
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
      <aside className="w-64 shrink-0 flex flex-col gap-4 p-4 bg-gray-900 border-r border-gray-800 overflow-y-auto">
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

        <div className="flex flex-col gap-1.5">
          <label className="text-xs text-gray-400 uppercase tracking-wider">Session ID</label>
          <div className="flex gap-1">
            <input
              type="text"
              value={sessionId ?? ''}
              onChange={(e) => setSessionId(e.target.value)}
              placeholder="Автоматически"
              className="flex-1 min-w-0 bg-gray-800 border border-gray-700 rounded-md px-3 py-2 text-xs text-gray-100 placeholder-gray-600 focus:outline-none focus:ring-1 focus:ring-blue-500 font-mono"
            />
            {sessionId && <CopyButton text={sessionId} />}
          </div>
          {sessionId && (
            <p className="text-xs text-gray-600 truncate" title={sessionId}>{sessionId}</p>
          )}
        </div>

        <ModelInfo />

        <TokenStatsPanel />

        <button
          onClick={clearSession}
          className="mt-auto px-3 py-2 text-sm text-gray-400 hover:text-white hover:bg-gray-800 rounded-md transition-colors text-left"
        >
          🗑 Новый разговор
        </button>
      </aside>

      <main className="flex flex-col flex-1 overflow-hidden">
        <ChatHistory />
        {error && (
          <div className="px-6 py-2 text-sm text-red-400 bg-red-950/30 border-t border-red-900">{error}</div>
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
