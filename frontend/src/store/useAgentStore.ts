import { create } from 'zustand'
import type { ContextStrategyType, BranchInfo, MemoryLayersSnapshot, TaskStateSnapshot } from '../api/agentApi'

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
  memoryEnabled: boolean
  compressionStats: CompressionStats | null
  memoryLayersSnapshot: MemoryLayersSnapshot | null
  taskState: TaskStateSnapshot | null
  branches: BranchInfo[]
  profileId: string
  addMessage: (msg: ChatMessage) => void
  setMessages: (msgs: ChatMessage[]) => void
  setSessionId: (id: string | null) => void
  setLoading: (v: boolean) => void
  setError: (v: string | null) => void
  setModel: (v: string | null) => void
  setTokenTotals: (t: TokenTotals) => void
  setStrategy: (s: ContextStrategyType) => void
  setMemoryEnabled: (v: boolean) => void
  setCompressionStats: (stats: CompressionStats) => void
  setMemoryLayersSnapshot: (snapshot: MemoryLayersSnapshot | null) => void
  setTaskState: (s: TaskStateSnapshot | null) => void
  setBranches: (branches: BranchInfo[]) => void
  addBranch: (branch: BranchInfo) => void
  switchToBranch: (branchSessionId: string) => void
  clearSession: () => void
  setProfileId: (id: string) => void
}

export const useAgentStore = create<AgentStore>((set, get) => ({
  messages: [],
  sessionId: null,
  isLoading: false,
  error: null,
  model: null,
  tokenTotals: null,
  strategy: 'FULL_HISTORY',
  memoryEnabled: true,
  compressionStats: null,
  memoryLayersSnapshot: null,
  taskState: null,
  branches: [],
  profileId: 'default',
  addMessage: (msg) => set((state) => ({ messages: [...state.messages, msg] })),
  setMessages: (messages) => set({ messages }),
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
  setMemoryEnabled: (memoryEnabled) => set({ memoryEnabled }),
  setCompressionStats: (compressionStats) => set({ compressionStats }),
  setMemoryLayersSnapshot: (memoryLayersSnapshot) => set({ memoryLayersSnapshot }),
  setTaskState: (taskState) => set({ taskState }),
  setBranches: (branches) => set({ branches }),
  addBranch: (branch) => set((state) => ({ branches: [...state.branches, branch] })),
  switchToBranch: (branchSessionId) => set((state) => ({
    sessionId: branchSessionId,
    messages: [],
    error: null,
    tokenTotals: null,
    compressionStats: null,
    memoryLayersSnapshot: null,
    taskState: null,
  })),
  clearSession: () => set({
    messages: [],
    sessionId: null,
    error: null,
    tokenTotals: null,
    compressionStats: null,
    memoryLayersSnapshot: null,
    taskState: null,
    branches: [],
    strategy: 'FULL_HISTORY',
    // profileId intentionally not reset
  }),
  setProfileId: (profileId) => set({ profileId }),
}))
