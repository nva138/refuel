import { describe, it, expect, vi, afterEach } from 'vitest'

import { flushPromises, mount } from '@vue/test-utils'
import App from '../App.vue'
import router from '../router'

describe('App', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renders the today view with the summary from the api', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          date: '2026-10-10',
          meals: [],
          totalKcal: 535,
          totalProtein: 20.4,
          totalCarbs: 91,
          totalFat: 8.1,
        }),
      }),
    )
    router.push('/')
    await router.isReady()

    const wrapper = mount(App, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Today')
    expect(wrapper.text()).toContain('535')
  })
})
