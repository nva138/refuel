import type { LogWeightRequest, Weight } from '@/types/weight'

export async function fetchWeightHistory(): Promise<Weight[]> {
  const response = await fetch('/api/weight')
  if (!response.ok) throw new Error(`Request failed: ${response.status}`)
  return response.json()
}

export async function logWeight(request: LogWeightRequest): Promise<Weight> {
  const response = await fetch('/api/weight', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  if (!response.ok) throw new Error(`Request failed: ${response.status}`)
  return response.json()
}
