import axios from 'axios'

export interface AgentChatRequest {
  message: string
  sessionId?: string
  model?: string | null
}

export interface AgentChatResponse {
  sessionId: string
  reply: string
  inputTokens: number
  outputTokens: number
  responseTimeMs: number
}

export async function sendAgentMessage(req: AgentChatRequest): Promise<AgentChatResponse> {
  const body: AgentChatRequest = { message: req.message }
  if (req.sessionId) body.sessionId = req.sessionId
  if (req.model) body.model = req.model
  const { data } = await axios.post<AgentChatResponse>('/api/agent/chat', body)
  return data
}
