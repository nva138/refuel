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

function stubFetch(energy: unknown) {
  vi.stubGlobal(
    'fetch',
    vi.fn((url: string) => {
      if (url === '/api/whoop/energy') {
        return Promise.resolve(
          energy
            ? { ok: true, status: 200, json: async () => energy }
            : { ok: false, status: 409, json: async () => ({}) },
        )
      }
      return Promise.resolve({ ok: true, status: 200, json: async () => responses[url] })
    }),
  )
}

async function mountToday() {
  router.push('/')
  await router.isReady()
  const wrapper = mount(App, { global: { plugins: [router] } })
  await flushPromises()
  return wrapper
}

describe('App', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('uses the manual burn as goal when whoop is not connected', async () => {
    stubFetch(null)

    const wrapper = await mountToday()

    expect(wrapper.text()).toContain('Today')
    expect(wrapper.text()).toContain('535')
    expect(wrapper.text()).toMatch(/of 2\D?700 kcal/)
    expect(wrapper.text()).toContain('Bulk')
    expect(wrapper.text()).not.toContain('Burned')
  })

  it('uses the whoop weekly average as goal when connected', async () => {
    stubFetch({ weeklyAverageKcal: 2301, todayKcal: 915 })

    const wrapper = await mountToday()

    expect(wrapper.text()).toMatch(/of 2\D?301 kcal/)
    expect(wrapper.text()).toContain('Burned')
    expect(wrapper.text()).toContain('915')
  })
})
