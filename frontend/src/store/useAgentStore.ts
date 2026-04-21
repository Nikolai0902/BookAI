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

interface CompressionStats {
  recentAsIs: number
  summarized: number
}

interface AgentStore {
  messages: ChatMessage[]
  sessionId: string | null
  isLoading: boolean
  error: string | null
  model: string | null
  tokenTotals: TokenTotals | null
  useCompression: boolean
  compressionStats: CompressionStats | null
  addMessage: (msg: ChatMessage) => void
  setSessionId: (id: string | null) => void
  setLoading: (v: boolean) => void
  setError: (v: string | null) => void
  setModel: (v: string | null) => void
  setTokenTotals: (t: TokenTotals) => void
  toggleCompression: () => void
  setCompressionStats: (stats: CompressionStats) => void
  clearSession: () => void
}

export const useAgentStore = create<AgentStore>((set, get) => ({
  messages: [],
  sessionId: null,
  isLoading: false,
  error: null,
  model: null,
  tokenTotals: null,
  useCompression: false,
  compressionStats: null,
  addMessage: (msg) => set((state) => ({ messages: [...state.messages, msg] })),
  setSessionId: (id) => set({ sessionId: id || null }),
  setLoading: (isLoading) => set({ isLoading }),
  setError: (error) => set({ error, isLoading: false }),
  setModel: (model) => set({ model }),
  setTokenTotals: (tokenTotals) => set({ tokenTotals }),
  toggleCompression: () => {
    if (get().messages.length === 0) {
      set((state) => ({ useCompression: !state.useCompression }))
    }
  },
  setCompressionStats: (compressionStats) => set({ compressionStats }),
  clearSession: () => set({ messages: [], sessionId: null, error: null, tokenTotals: null, compressionStats: null }),
}))
