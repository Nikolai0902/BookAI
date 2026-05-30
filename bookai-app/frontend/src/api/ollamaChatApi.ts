import axios from 'axios'

export interface HistoryMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface OllamaOptions {
  temperature?: number | null
  topP?: number | null
  topK?: number | null
  numCtx?: number | null
  numPredict?: number | null
  repeatPenalty?: number | null
}

export interface OllamaChatRequest {
  message: string
  history: HistoryMessage[]
  model?: string | null
  systemPrompt?: string | null
  options?: OllamaOptions | null
}

export interface OllamaChatResponse {
  answer: string
  model: string
  elapsedMs: number
  evalCount: number
  appliedOptions?: Record<string, number | string>
}

export interface OllamaStatus {
  available: boolean
  model: string
  baseUrl: string
}

export async function sendOllamaMessage(req: OllamaChatRequest): Promise<OllamaChatResponse> {
  const { data } = await axios.post<OllamaChatResponse>('/api/ollama-chat/chat', req)
  return data
}

export async function checkOllamaStatus(): Promise<OllamaStatus> {
  const { data } = await axios.get<OllamaStatus>('/api/ollama-chat/status')
  return data
}
