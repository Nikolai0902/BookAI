import { create } from 'zustand'
import type { OllamaOptions } from '../api/ollamaChatApi'

export interface OllamaParams {
  model: string
  systemPrompt: string
  promptPreset: 'default' | 'rag'
  options: OllamaOptions
}

export interface OllamaMessage {
  role: 'user' | 'assistant'
  content: string
  elapsedMs?: number
  evalCount?: number
  model?: string
  appliedOptions?: Record<string, number | string>
  paramsSnapshot?: Pick<OllamaParams, 'model' | 'promptPreset'>
}

interface OllamaStatus {
  available: boolean
  model: string
}

interface OllamaChatStore {
  messages: OllamaMessage[]
  isLoading: boolean
  error: string | null
  modelInfo: OllamaStatus | null
  params: OllamaParams
  addMessage: (msg: OllamaMessage) => void
  setLoading: (v: boolean) => void
  setError: (v: string | null) => void
  setModelInfo: (info: OllamaStatus | null) => void
  setParams: (patch: Partial<OllamaParams>) => void
  setOptions: (patch: Partial<OllamaOptions>) => void
  resetParams: () => void
  clearMessages: () => void
}

export const DEFAULT_PARAMS: OllamaParams = {
  model: '',
  systemPrompt: '',
  promptPreset: 'default',
  options: {
    temperature: 0.2,
    topP: 0.9,
    topK: 40,
    numCtx: 4096,
    numPredict: 512,
    repeatPenalty: 1.1,
  },
}

export const RAG_SYSTEM_PROMPT = `Ты отвечаешь на вопрос ТОЛЬКО по тексту фрагментов ниже.

Правила:
- Не придумывай факты. Если ответа нет во фрагментах — пиши ровно: "Недостаточно информации."
- Каждый факт подтверждай цитатой в формате: "...текст..." [N], где N — номер фрагмента.
- В конце добавь блок "Источники:" со списком: [N] source | section.`

export const useOllamaChatStore = create<OllamaChatStore>((set) => ({
  messages: [],
  isLoading: false,
  error: null,
  modelInfo: null,
  params: DEFAULT_PARAMS,
  addMessage: (msg) => set((s) => ({ messages: [...s.messages, msg] })),
  setLoading: (v) => set({ isLoading: v }),
  setError: (v) => set({ error: v }),
  setModelInfo: (info) => set({ modelInfo: info }),
  setParams: (patch) => set((s) => ({ params: { ...s.params, ...patch } })),
  setOptions: (patch) =>
    set((s) => ({ params: { ...s.params, options: { ...s.params.options, ...patch } } })),
  resetParams: () => set({ params: DEFAULT_PARAMS }),
  clearMessages: () => set({ messages: [], error: null }),
}))
