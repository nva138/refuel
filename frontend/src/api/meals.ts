import type {
  DaySummary,
  CreateMealRequest,
  Meal,
  SuggestMealRequest,
  SuggestMealResponse,
} from '@/types/meal'

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

export async function suggestMeal(request: SuggestMealRequest): Promise<SuggestMealResponse> {
  const response = await fetch('/api/meals/suggest', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  if (!response.ok) throw new Error(`Request failed: ${response.status}`)
  return response.json()
}

export async function suggestMealFromPhoto(
  photo: Blob,
  text: string,
): Promise<SuggestMealResponse> {
  const formData = new FormData()
  formData.append('photo', photo, 'meal.jpg')
  if (text.trim() !== '') {
    formData.append('text', text)
  }
  const response = await fetch('/api/meals/suggest-photo', {
    method: 'POST',
    body: formData,
  })
  if (!response.ok) throw new Error(`Request failed: ${response.status}`)
  return response.json()
}

export async function deleteMeal(id: number): Promise<void> {
  const response = await fetch(`/api/meals/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`Request failed: ${response.status}`)
}
