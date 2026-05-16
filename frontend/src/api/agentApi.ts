import axios from 'axios'

export type ContextStrategyType =
  | 'FULL_HISTORY'
  | 'COMPRESSION'
  | 'SLIDING_WINDOW'
  | 'BRANCHING'

export type CommunicationStyle = 'FORMAL' | 'CASUAL' | 'TECHNICAL' | 'CONCISE'
export type ResponseFormat = 'PLAIN' | 'MARKDOWN' | 'BULLETS' | 'DETAILED'

export interface UserProfile {
  profileId: string
  displayName: string
  style: CommunicationStyle
  responseFormat: ResponseFormat
  constraints: string | null
}

export interface AgentChatRequest {
  message: string
  sessionId?: string
  model?: string | null
  strategy?: ContextStrategyType
  memoryEnabled?: boolean
  profileId?: string
}

export interface MemoryLayersSnapshot {
  shortTermCount: number
  workingMemory: string | null
  longTermMemory: Record<string, string>
}

export type TaskPhase = 'NONE' | 'PLANNING' | 'EXECUTION' | 'VALIDATION' | 'DONE'

export interface TaskStateSnapshot {
  phase: TaskPhase
  currentStep: string | null
  expectedAction: string | null
  paused: boolean
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
  lastMessageId: number
  memoryLayersSnapshot: MemoryLayersSnapshot
  taskState: TaskStateSnapshot | null
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
    memoryEnabled: req.memoryEnabled ?? true,
  }
  if (req.sessionId) body.sessionId = req.sessionId
  if (req.model) body.model = req.model
  if (req.profileId) body.profileId = req.profileId
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

export async function clearLongTermMemory(profileId = 'default'): Promise<void> {
  await axios.delete(`/api/memory/longterm?profileId=${profileId}`)
}

export async function fetchProfiles(): Promise<UserProfile[]> {
  const { data } = await axios.get<UserProfile[]>('/api/profiles')
  return data
}

export async function createProfile(profile: UserProfile): Promise<UserProfile> {
  const { data } = await axios.post<UserProfile>('/api/profiles', profile)
  return data
}

export async function updateProfile(profileId: string, profile: Partial<UserProfile>): Promise<UserProfile> {
  const { data } = await axios.put<UserProfile>(`/api/profiles/${profileId}`, profile)
  return data
}

export async function deleteProfile(profileId: string): Promise<void> {
  await axios.delete(`/api/profiles/${profileId}`)
}

export interface SessionSummary {
  sessionId: string
  lastMessage: string
  lastMessageAt: string
  turnCount: number
}

export interface SessionMessageDto {
  role: 'user' | 'assistant'
  content: string
  inputTokens?: number
  outputTokens?: number
  messageId?: number
}

export async function getRecentSessions(limit = 5): Promise<SessionSummary[]> {
  const { data } = await axios.get<SessionSummary[]>(`/api/agent/sessions?limit=${limit}`)
  return data
}

export async function getSessionMessages(sessionId: string): Promise<SessionMessageDto[]> {
  const { data } = await axios.get<SessionMessageDto[]>(`/api/agent/sessions/${sessionId}/messages`)
  return data
}

export async function pauseTask(sessionId: string): Promise<void> {
  await axios.put(`/api/task/${sessionId}/pause`)
}

export async function resumeTask(sessionId: string): Promise<void> {
  await axios.put(`/api/task/${sessionId}/resume`)
}
