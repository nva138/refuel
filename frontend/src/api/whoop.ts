import type { Energy } from '@/types/whoop'

export async function fetchEnergy(date?: string): Promise<Energy | null> {
  const response = await fetch(date ? `/api/whoop/energy?date=${date}` : '/api/whoop/energy')
  if (response.status === 409) return null
  if (!response.ok) throw new Error(`Request failed: ${response.status}`)
  return response.json()
}
