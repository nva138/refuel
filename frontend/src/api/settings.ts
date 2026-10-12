import type { Settings, UpdateSettingsRequest } from '@/types/settings'

export async function fetchSettings(): Promise<Settings> {
  const response = await fetch('/api/settings')
  if (!response.ok) throw new Error(`Request failed: ${response.status}`)
  return response.json()
}

export async function updateSettings(request: UpdateSettingsRequest): Promise<Settings> {
  const response = await fetch('/api/settings', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  if (!response.ok) throw new Error(`Request failed: ${response.status}`)
  return response.json()
}
