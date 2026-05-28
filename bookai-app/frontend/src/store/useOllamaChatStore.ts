import { create } from 'zustand'

export interface OllamaMessage {
  role: 'user' | 'assistant'
  content: string
  elapsedMs?: number
  evalCount?: number
  model?: string
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
  addMessage: (msg: OllamaMessage) => void
  setLoading: (v: boolean) => void
  setError: (v: string | null) => void
  setModelInfo: (info: OllamaStatus | null) => void
  clearMessages: () => void
}

export const useOllamaChatStore = create<OllamaChatStore>((set) => ({
  messages: [],
  isLoading: false,
  error: null,
  modelInfo: null,
  addMessage: (msg) => set((s) => ({ messages: [...s.messages, msg] })),
  setLoading: (v) => set({ isLoading: v }),
  setError: (v) => set({ error: v }),
  setModelInfo: (info) => set({ modelInfo: info }),
  clearMessages: () => set({ messages: [], error: null }),
}))
