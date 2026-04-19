import { useRef } from 'react'
import { useAppStore } from '../../store/useAppStore'
import { sendRequest } from '../../api/bookApi'

export default function PromptInput() {
  const { prompt, mode, strategy, loading, setPrompt, setLoading, setResponse, setError, addToHistory } =
    useAppStore()
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  const handleSubmit = async () => {
    if (!prompt.trim() || loading) return

    setLoading(true)
    try {
      const { answer, inputTokens, outputTokens } = await sendRequest(prompt, mode, strategy)
      setResponse(answer, inputTokens, outputTokens)
      addToHistory({
        id: crypto.randomUUID(),
        prompt,
        mode,
        strategy: mode === 'REASON' ? strategy : undefined,
        answer,
        timestamp: Date.now(),
      })
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Ошибка запроса')
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && e.ctrlKey) handleSubmit()
  }

  const autoResize = () => {
    const el = textareaRef.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = `${Math.min(el.scrollHeight, 200)}px`
  }

  return (
    <div className="p-4 border-t border-gray-800 bg-gray-950">
      <div className="flex gap-2 items-end">
        <textarea
          ref={textareaRef}
          value={prompt}
          onChange={(e) => { setPrompt(e.target.value); autoResize() }}
          onKeyDown={handleKeyDown}
          placeholder="Введите запрос… (Ctrl+Enter для отправки)"
          rows={1}
          className="flex-1 bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm resize-none focus:outline-none focus:ring-1 focus:ring-blue-500 placeholder-gray-500 text-gray-100"
          style={{ minHeight: '40px' }}
        />
        <button
          onClick={handleSubmit}
          disabled={!prompt.trim() || loading}
          className="px-4 py-2 bg-blue-600 hover:bg-blue-500 disabled:bg-gray-700 disabled:text-gray-500 rounded-lg text-sm font-medium transition-colors whitespace-nowrap"
        >
          {loading ? 'Генерирую…' : 'Отправить'}
        </button>
      </div>
    </div>
  )
}
