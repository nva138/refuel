import type {
  DaySummary,
  CreateMealRequest,
  Meal,
  SuggestMealRequest,
  SuggestMealResponse,
  ReadLabelResponse,
} from '@/types/meal'

export async function fetchToday(): Promise<DaySummary> {
  const response = await fetch('/api/meals/today')
  if (!response.ok) throw new Error(`Request failed: ${response.status}`)
  return response.json()
}

export async function fetchDay(date: string): Promise<DaySummary> {
  const response = await fetch(`/api/meals/day?date=${date}`)
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

export async function updatePartAmount(
  mealId: number,
  partId: number,
  amountG: number,
): Promise<Meal> {
  const response = await fetch(`/api/meals/${mealId}/parts/${partId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ amountG }),
  })
  if (!response.ok) throw new Error(`Request failed: ${response.status}`)
  return response.json()
}

export async function fetchFavourites(): Promise<Meal[]> {
  const response = await fetch('/api/meals/favourites')
  if (!response.ok) throw new Error(`Request failed: ${response.status}`)
  return response.json()
}

export async function setFavourite(id: number, favourite: boolean): Promise<Meal> {
  const response = await fetch(`/api/meals/${id}/favourite`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ favourite }),
  })
  if (!response.ok) throw new Error(`Request failed: ${response.status}`)
  return response.json()
}

export async function readLabel(photo: Blob): Promise<ReadLabelResponse> {
  const formData = new FormData()
  formData.append('photo', photo, 'part.jpg')
  const response = await fetch('/api/meals/read-label', {
    method: 'POST',
    body: formData,
  })
  if (!response.ok) throw new Error(`Request failed: ${response.status}`)
  return response.json()
}
