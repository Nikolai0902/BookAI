import axios from 'axios'

export interface AgentChatRequest {
  message: string
  sessionId?: string
  model?: string | null
  useCompression?: boolean
}

export interface AgentChatResponse {
  sessionId: string
  reply: string
  inputTokens: number
  outputTokens: number
  responseTimeMs: number
  totalInputTokens: number
  totalOutputTokens: number
  turnNumber: number
  compressionEnabled: boolean
  recentMessagesAsIs: number
  summarizedMessagesCount: number
}

export async function sendAgentMessage(req: AgentChatRequest): Promise<AgentChatResponse> {
  const body: AgentChatRequest = {
    message: req.message,
    useCompression: req.useCompression ?? false,
  }
  if (req.sessionId) body.sessionId = req.sessionId
  if (req.model) body.model = req.model
  const { data } = await axios.post<AgentChatResponse>('/api/agent/chat', body)
  return data
}
