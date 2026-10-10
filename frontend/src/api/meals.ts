import type { DaySummary, CreateMealRequest, Meal } from '@/types/meal'

export async function fetchToday(): Promise<DaySummary> {
  const response = await fetch('/api/meals/today')
  if (!response.ok) throw new Error(`Request failed: ${response.status}`)
  return response.json()
}

export async function createMeal(request: CreateMealRequest): Promise<Meal> {
  const response = await fetch('/api/meals', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  if (!response.ok) throw new Error(`Request failed: ${response.status}`)
  return response.json()
}
