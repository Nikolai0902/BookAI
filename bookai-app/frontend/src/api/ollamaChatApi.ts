import axios from 'axios'

export interface HistoryMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface OllamaChatRequest {
  message: string
  history: HistoryMessage[]
}

export interface OllamaChatResponse {
  answer: string
  model: string
  elapsedMs: number
  evalCount: number
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
