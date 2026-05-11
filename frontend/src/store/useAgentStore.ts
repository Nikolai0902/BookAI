import { create } from 'zustand'
import type { ContextStrategyType, BranchInfo } from '../api/agentApi'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  inputTokens?: number
  outputTokens?: number
  totalInputTokens?: number
  totalOutputTokens?: number
  turnNumber?: number
  responseTimeMs?: number
  lastMessageId?: number
}

interface TokenTotals {
  totalInputTokens: number
  totalOutputTokens: number
  turnNumber: number
}

interface CompressionStats {
  recentMessagesCount: number
  summarized: number
}

interface AgentStore {
  messages: ChatMessage[]
  sessionId: string | null
  isLoading: boolean
  error: string | null
  model: string | null
  tokenTotals: TokenTotals | null
  strategy: ContextStrategyType
  compressionStats: CompressionStats | null
  factsSnapshot: string | null
  branches: BranchInfo[]
  addMessage: (msg: ChatMessage) => void
  setSessionId: (id: string | null) => void
  setLoading: (v: boolean) => void
  setError: (v: string | null) => void
  setModel: (v: string | null) => void
  setTokenTotals: (t: TokenTotals) => void
  setStrategy: (s: ContextStrategyType) => void
  setCompressionStats: (stats: CompressionStats) => void
  setFacts: (facts: string | null) => void
  setBranches: (branches: BranchInfo[]) => void
  addBranch: (branch: BranchInfo) => void
  switchToBranch: (branchSessionId: string) => void
  clearSession: () => void
}

export const useAgentStore = create<AgentStore>((set, get) => ({
  messages: [],
  sessionId: null,
  isLoading: false,
  error: null,
  model: null,
  tokenTotals: null,
  strategy: 'FULL_HISTORY',
  compressionStats: null,
  factsSnapshot: null,
  branches: [],
  addMessage: (msg) => set((state) => ({ messages: [...state.messages, msg] })),
  setSessionId: (id) => set({ sessionId: id || null }),
  setLoading: (isLoading) => set({ isLoading }),
  setError: (error) => set({ error, isLoading: false }),
  setModel: (model) => set({ model }),
  setTokenTotals: (tokenTotals) => set({ tokenTotals }),
  setStrategy: (strategy) => {
    if (get().messages.length === 0) {
      set({ strategy })
    }
  },
  setCompressionStats: (compressionStats) => set({ compressionStats }),
  setFacts: (factsSnapshot) => set({ factsSnapshot }),
  setBranches: (branches) => set({ branches }),
  addBranch: (branch) => set((state) => ({ branches: [...state.branches, branch] })),
  switchToBranch: (branchSessionId) => set((state) => ({
    sessionId: branchSessionId,
    messages: [],
    error: null,
    tokenTotals: null,
    compressionStats: null,
    factsSnapshot: null,
  })),
  clearSession: () => set({
    messages: [],
    sessionId: null,
    error: null,
    tokenTotals: null,
    compressionStats: null,
    factsSnapshot: null,
    branches: [],
    strategy: 'FULL_HISTORY',
  }),
}))
