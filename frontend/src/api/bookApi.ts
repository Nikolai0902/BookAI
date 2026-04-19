import axios from 'axios'
import type { BookRequest, BookResponse } from '../types/api'
import type { Mode, Strategy } from '../store/useAppStore'

export async function sendRequest(prompt: string, mode: Mode, strategy: Strategy, temperature: number | null): Promise<BookResponse> {
  const body: BookRequest = { prompt }

  if (temperature !== null) {
    body.temperature = temperature
  }

  if (mode === 'COMPARE') {
    body.filter = { type: 'COMPARE' }
  } else if (mode === 'REASON') {
    body.filter = { type: 'REASON', strategy }
  }

  const { data } = await axios.post<BookResponse>('/api/book', body)
  return data
}
