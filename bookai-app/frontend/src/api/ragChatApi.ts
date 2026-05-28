import axios from 'axios'

export type ChunkingStrategyType = 'FIXED_SIZE' | 'STRUCTURE'

export interface Citation {
  rank: number
  source: string
  section: string
  score: number
  snippet: string
}

export interface RagChatRequest {
  message: string
  sessionId?: string
  topK?: number
  minScore?: number
  rewriteQuery?: boolean
  strategy?: ChunkingStrategyType
  model?: string
}

export interface RagChatResponse {
  sessionId: string
  answer: string
  citations: Citation[]
  inputTokens: number
  outputTokens: number
  elapsedMs: number
  retrievedChunks: number
  filteredChunks: number
  confident: boolean
  turnNumber: number
  contextFacts: string | null
}

export async function sendRagChatMessage(req: RagChatRequest): Promise<RagChatResponse> {
  const { data } = await axios.post<RagChatResponse>('/api/rag-chat/chat', req)
  return data
}

export async function clearRagChatSession(sessionId: string): Promise<void> {
  await axios.delete(`/api/rag-chat/session/${sessionId}`)
}
