export type FilterType = 'COMPARE' | 'REASON'
export type ReasoningStrategy = 'DIRECT' | 'STEP_BY_STEP' | 'META_PROMPT' | 'EXPERT_PANEL'

export interface BookRequest {
  prompt: string
  temperature?: number
  filter?: {
    type: FilterType
    strategy?: ReasoningStrategy
  }
}

export interface BookResponse {
  answer: string
  inputTokens: number
  outputTokens: number
}

export interface CompareData {
  freeAnswer: string
  controlledAnswer: string
}
