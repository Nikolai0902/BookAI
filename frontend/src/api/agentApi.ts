import axios from 'axios'

export type ContextStrategyType =
  | 'FULL_HISTORY'
  | 'COMPRESSION'
  | 'SLIDING_WINDOW'
  | 'STICKY_FACTS'
  | 'BRANCHING'

export interface AgentChatRequest {
  message: string
  sessionId?: string
  model?: string | null
  strategy?: ContextStrategyType
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
  strategy: ContextStrategyType
  recentMessagesCount: number
  summarizedMessagesCount: number
  factsSnapshot?: string | null
  lastMessageId: number
}

export interface BranchCreateRequest {
  rootSessionId: string
  checkpointMessageId: number
  label: string
}

export interface BranchInfo {
  branchSessionId: string
  rootSessionId: string
  checkpointMessageId: number
  label: string
  createdAt: string
}

export async function sendAgentMessage(req: AgentChatRequest): Promise<AgentChatResponse> {
  const body: AgentChatRequest = {
    message: req.message,
    strategy: req.strategy ?? 'FULL_HISTORY',
  }
  if (req.sessionId) body.sessionId = req.sessionId
  if (req.model) body.model = req.model
  const { data } = await axios.post<AgentChatResponse>('/api/agent/chat', body)
  return data
}

export async function createBranch(req: BranchCreateRequest): Promise<BranchInfo> {
  const { data } = await axios.post<BranchInfo>('/api/agent/branch', req)
  return data
}

export async function listBranches(rootSessionId: string): Promise<BranchInfo[]> {
  const { data } = await axios.get<BranchInfo[]>(`/api/agent/branch/${rootSessionId}`)
  return data
}
