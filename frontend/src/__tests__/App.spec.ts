import { describe, it, expect, vi, afterEach } from 'vitest'

import { flushPromises, mount } from '@vue/test-utils'
import App from '../App.vue'
import router from '../router'

const responses: Record<string, unknown> = {
  '/api/meals/today': {
    date: '2026-10-10',
    meals: [],
    totalKcal: 535,
    totalProtein: 20.4,
    totalCarbs: 91,
    totalFat: 8.1,
  },
  '/api/settings': {
    mode: 'BULK',
    bodyWeightKg: 95,
    kcalReference: 2700,
    proteinGoalG: 190,
  },
}

describe('App', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renders today against the goals from the settings', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => Promise.resolve({ ok: true, json: async () => responses[url] })),
    )
    router.push('/')
    await router.isReady()

    const wrapper = mount(App, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Today')
    expect(wrapper.text()).toContain('535')
    expect(wrapper.text()).toMatch(/of 2\D?700 kcal/)
    expect(wrapper.text()).toContain('Bulk')
  })
})
