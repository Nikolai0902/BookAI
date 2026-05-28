import { create } from 'zustand'
import type { Citation } from '../api/ragChatApi'

export interface RagChatMessage {
  role: 'user' | 'assistant'
  content: string
  citations?: Citation[]
  confident?: boolean
  inputTokens?: number
  outputTokens?: number
  turnNumber?: number
  elapsedMs?: number
}

interface RagChatStore {
  messages: RagChatMessage[]
  sessionId: string | null
  isLoading: boolean
  error: string | null
  contextFacts: string | null
  addMessage: (msg: RagChatMessage) => void
  setSessionId: (id: string | null) => void
  setLoading: (v: boolean) => void
  setError: (v: string | null) => void
  setContextFacts: (facts: string | null) => void
  clearSession: () => void
}

export const useRagChatStore = create<RagChatStore>((set) => ({
  messages: [],
  sessionId: null,
  isLoading: false,
  error: null,
  contextFacts: null,
  addMessage: (msg) => set((state) => ({ messages: [...state.messages, msg] })),
  setSessionId: (sessionId) => set({ sessionId }),
  setLoading: (isLoading) => set({ isLoading }),
  setError: (error) => set({ error, isLoading: false }),
  setContextFacts: (contextFacts) => set({ contextFacts }),
  clearSession: () => set({
    messages: [],
    sessionId: null,
    error: null,
    contextFacts: null,
    isLoading: false,
  }),
}))
