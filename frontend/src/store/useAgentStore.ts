import { create } from 'zustand'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  inputTokens?: number
  outputTokens?: number
  totalInputTokens?: number
  totalOutputTokens?: number
  turnNumber?: number
  responseTimeMs?: number
}

interface TokenTotals {
  totalInputTokens: number
  totalOutputTokens: number
  turnNumber: number
}

interface AgentStore {
  messages: ChatMessage[]
  sessionId: string | null
  isLoading: boolean
  error: string | null
  model: string | null
  tokenTotals: TokenTotals | null
  addMessage: (msg: ChatMessage) => void
  setSessionId: (id: string | null) => void
  setLoading: (v: boolean) => void
  setError: (v: string | null) => void
  setModel: (v: string | null) => void
  setTokenTotals: (t: TokenTotals) => void
  clearSession: () => void
}

export const useAgentStore = create<AgentStore>((set) => ({
  messages: [],
  sessionId: null,
  isLoading: false,
  error: null,
  model: null,
  tokenTotals: null,
  addMessage: (msg) => set((state) => ({ messages: [...state.messages, msg] })),
  setSessionId: (id) => set({ sessionId: id || null }),
  setLoading: (isLoading) => set({ isLoading }),
  setError: (error) => set({ error, isLoading: false }),
  setModel: (model) => set({ model }),
  setTokenTotals: (tokenTotals) => set({ tokenTotals }),
  clearSession: () => set({ messages: [], sessionId: null, error: null, tokenTotals: null }),
}))
