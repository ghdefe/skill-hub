import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import HomePage from './HomePage.vue'

// Mock http module
vi.mock('@/api/http', () => ({
  default: {
    get: vi.fn(),
  },
}))

import http from '@/api/http'

const mockHttp = vi.mocked(http)

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: HomePage },
      { path: '/skills/:id', component: { template: '<div />' } },
    ],
  })
}

describe('HomePage', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('fetches skills and tags on mount', async () => {
    mockHttp.get.mockImplementation((url: string) => {
      if (url === '/tags') {
        return Promise.resolve({ data: [{ id: '1', name: 'nlp' }] })
      }
      return Promise.resolve({
        data: {
          items: [
            {
              id: 's1',
              name: 'Test Skill',
              description: 'A test skill',
              author: 'user1',
              downloadCount: 10,
              starCount: 5,
              forkCount: 2,
              tags: ['nlp'],
              createdAt: '2024-01-01T00:00:00',
            },
          ],
          total: 1,
          page: 1,
          pageSize: 20,
        },
      })
    })

    const router = createTestRouter()
    await router.push('/')
    await router.isReady()

    const wrapper = mount(HomePage, { global: { plugins: [router] } })
    await flushPromises()

    expect(mockHttp.get).toHaveBeenCalledWith('/tags')
    expect(mockHttp.get).toHaveBeenCalledWith('/skills', expect.objectContaining({
      params: expect.objectContaining({ page: 1, pageSize: 20 }),
    }))

    expect(wrapper.text()).toContain('Test Skill')
    expect(wrapper.text()).toContain('user1')
  })

  it('debounces search input by 500ms', async () => {
    mockHttp.get.mockImplementation((url: string) => {
      if (url === '/tags') return Promise.resolve({ data: [] })
      return Promise.resolve({ data: { items: [], total: 0, page: 1, pageSize: 20 } })
    })

    const router = createTestRouter()
    await router.push('/')
    await router.isReady()

    const wrapper = mount(HomePage, { global: { plugins: [router] } })
    await flushPromises()

    // Clear initial call count
    const initialCallCount = mockHttp.get.mock.calls.filter(c => c[0] === '/skills').length

    // Type in search box
    const input = wrapper.find('input[type="text"]')
    await input.setValue('agent')
    await input.trigger('input')

    // Should not have called yet
    expect(mockHttp.get.mock.calls.filter(c => c[0] === '/skills').length).toBe(initialCallCount)

    // Advance 500ms
    vi.advanceTimersByTime(500)
    await flushPromises()

    // Now it should have called
    const afterCalls = mockHttp.get.mock.calls.filter(c => c[0] === '/skills')
    expect(afterCalls.length).toBe(initialCallCount + 1)
    expect(afterCalls[afterCalls.length - 1][1]).toEqual(
      expect.objectContaining({
        params: expect.objectContaining({ q: 'agent' }),
      })
    )
  })

  it('shows empty state when no skills found', async () => {
    mockHttp.get.mockImplementation((url: string) => {
      if (url === '/tags') return Promise.resolve({ data: [] })
      return Promise.resolve({ data: { items: [], total: 0, page: 1, pageSize: 20 } })
    })

    const router = createTestRouter()
    await router.push('/')
    await router.isReady()

    const wrapper = mount(HomePage, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.text()).toContain('未找到匹配的 Skills')
  })

  it('renders tag filter chips and toggles selection', async () => {
    mockHttp.get.mockImplementation((url: string) => {
      if (url === '/tags') {
        return Promise.resolve({
          data: [
            { id: '1', name: 'nlp' },
            { id: '2', name: 'chat' },
          ],
        })
      }
      return Promise.resolve({ data: { items: [], total: 0, page: 1, pageSize: 20 } })
    })

    const router = createTestRouter()
    await router.push('/')
    await router.isReady()

    const wrapper = mount(HomePage, { global: { plugins: [router] } })
    await flushPromises()

    const tagButtons = wrapper.findAll('button').filter(b => ['nlp', 'chat'].includes(b.text()))
    expect(tagButtons.length).toBe(2)

    // Click a tag to select it
    await tagButtons[0].trigger('click')
    await flushPromises()

    // Verify the API was called with the tag filter
    const skillCalls = mockHttp.get.mock.calls.filter(c => c[0] === '/skills')
    const lastCall = skillCalls[skillCalls.length - 1]
    expect(lastCall[1]).toEqual(
      expect.objectContaining({
        params: expect.objectContaining({ tags: 'nlp' }),
      })
    )
  })

  it('sends sort parameters in API call', async () => {
    mockHttp.get.mockImplementation((url: string) => {
      if (url === '/tags') return Promise.resolve({ data: [] })
      return Promise.resolve({ data: { items: [], total: 0, page: 1, pageSize: 20 } })
    })

    const router = createTestRouter()
    await router.push('/')
    await router.isReady()

    const wrapper = mount(HomePage, { global: { plugins: [router] } })
    await flushPromises()

    // Default sort should be createdAt desc
    const initialCalls = mockHttp.get.mock.calls.filter(c => c[0] === '/skills')
    expect(initialCalls[0][1]).toEqual(
      expect.objectContaining({
        params: expect.objectContaining({ sort: 'createdAt', order: 'desc' }),
      })
    )
  })
})
