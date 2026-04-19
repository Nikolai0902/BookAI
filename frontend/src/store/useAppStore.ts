import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type Mode = 'ASK' | 'COMPARE' | 'REASON'
export type Strategy = 'DIRECT' | 'STEP_BY_STEP' | 'META_PROMPT' | 'EXPERT_PANEL'

export interface HistoryEntry {
  id: string
  prompt: string
  mode: Mode
  strategy?: Strategy
  answer: string
  timestamp: number
}

interface AppStore {
  mode: Mode
  strategy: Strategy
  prompt: string
  temperature: number | null
  model: string | null
  loading: boolean
  response: string | null
  error: string | null
  inputTokens: number | null
  outputTokens: number | null
  responseTimeMs: number | null
  costUsd: number | null
  history: HistoryEntry[]
  setMode: (mode: Mode) => void
  setStrategy: (strategy: Strategy) => void
  setPrompt: (prompt: string) => void
  setTemperature: (v: number | null) => void
  setModel: (v: string | null) => void
  setLoading: (v: boolean) => void
  setResponse: (v: string | null, inputTokens?: number | null, outputTokens?: number | null, responseTimeMs?: number | null, costUsd?: number | null) => void
  setError: (v: string | null) => void
  addToHistory: (entry: HistoryEntry) => void
  restoreFromHistory: (entry: HistoryEntry) => void
}

export const useAppStore = create<AppStore>()(
  persist(
    (set) => ({
      mode: 'ASK',
      strategy: 'DIRECT',
      prompt: '',
      temperature: null,
      model: null,
      loading: false,
      response: null,
      error: null,
      inputTokens: null,
      outputTokens: null,
      responseTimeMs: null,
      costUsd: null,
      history: [],
      setMode: (mode) => set({ mode, response: null, error: null, inputTokens: null, outputTokens: null, responseTimeMs: null, costUsd: null }),
      setStrategy: (strategy) => set({ strategy }),
      setPrompt: (prompt) => set({ prompt }),
      setTemperature: (temperature) => set({ temperature }),
      setModel: (model) => set({ model }),
      setLoading: (loading) => set({ loading }),
      setResponse: (response, inputTokens = null, outputTokens = null, responseTimeMs = null, costUsd = null) =>
        set({ response, error: null, inputTokens, outputTokens, responseTimeMs, costUsd }),
      setError: (error) => set({ error, loading: false }),
      addToHistory: (entry) =>
        set((state) => ({ history: [entry, ...state.history].slice(0, 20) })),
      restoreFromHistory: (entry) =>
        set({
          mode: entry.mode,
          strategy: entry.strategy ?? 'DIRECT',
          prompt: entry.prompt,
          response: entry.answer,
          error: null,
        }),
    }),
    {
      name: 'bookai-store',
      partialize: (state) => ({ history: state.history }),
    }
  )
)
