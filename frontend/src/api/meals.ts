import type { DaySummary } from '@/types/meal'

export async function fetchToday(): Promise<DaySummary> {
  const response = await fetch('/api/meals/today')
  if (!response.ok) throw new Error(`Request failed: ${response.status}`)
  return response.json()
}
