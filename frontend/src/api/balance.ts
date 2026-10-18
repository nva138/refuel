import type { WeekBalance } from '@/types/balance'

export async function fetchWeekBalance(until?: string): Promise<WeekBalance> {
  const response = await fetch(until ? `/api/balance?until=${until}` : '/api/balance')
  if (!response.ok) throw new Error(`Request failed: ${response.status}`)
  return response.json()
}
