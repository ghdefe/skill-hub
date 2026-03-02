import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import RepositoriesPage from './RepositoriesPage.vue'

vi.mock('@/api/http', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
  },
}))

import http from '@/api/http'

const mockHttp = vi.mocked(http)

const mockRepo = {
  id: 'r1',
  githubOwner: 'owner',
  githubRepo: 'repo',
  url: 'https://github.com/owner/repo',
  starCount: 10,
  forkCount: 3,
  lastSyncedAt: '2024-01-01T00:00:00',
  createdAt: '2024-01-01T00:00:00',
  skillGroup: { id: 'sg1', name: 'repo', skillCount: 2 },
}

describe('RepositoriesPage - Sync Button', () => {
  beforeEach(() => {
    mockHttp.get.mockResolvedValue({ data: [mockRepo] })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders a sync button for each repository', async () => {
    const wrapper = mount(RepositoriesPage)
    await flushPromises()

    const syncBtn = wrapper.findAll('button').find(b => b.text().includes('同步'))
    expect(syncBtn).toBeTruthy()
    expect(syncBtn!.text()).toBe('同步')
  })

  it('calls POST /api/repositories/{id}/sync on click', async () => {
    const updatedRepo = { ...mockRepo, starCount: 20, forkCount: 5 }
    mockHttp.post.mockResolvedValue({ data: updatedRepo })

    const wrapper = mount(RepositoriesPage)
    await flushPromises()

    const syncBtn = wrapper.findAll('button').find(b => b.text() === '同步')!
    await syncBtn.trigger('click')
    await flushPromises()

    expect(mockHttp.post).toHaveBeenCalledWith('/repositories/r1/sync')
  })

  it('shows loading state while syncing', async () => {
    let resolveSync: (value: unknown) => void
    mockHttp.post.mockReturnValue(new Promise(r => { resolveSync = r }))

    const wrapper = mount(RepositoriesPage)
    await flushPromises()

    const syncBtn = wrapper.findAll('button').find(b => b.text() === '同步')!
    await syncBtn.trigger('click')
    await flushPromises()

    const syncingBtn = wrapper.findAll('button').find(b => b.text().includes('同步中'))
    expect(syncingBtn).toBeTruthy()
    expect(syncingBtn!.attributes('disabled')).toBeDefined()

    resolveSync!({ data: mockRepo })
    await flushPromises()

    const normalBtn = wrapper.findAll('button').find(b => b.text() === '同步')
    expect(normalBtn).toBeTruthy()
  })

  it('updates repository data on successful sync', async () => {
    const updatedRepo = { ...mockRepo, starCount: 99, forkCount: 42 }
    mockHttp.post.mockResolvedValue({ data: updatedRepo })

    const wrapper = mount(RepositoriesPage)
    await flushPromises()

    const syncBtn = wrapper.findAll('button').find(b => b.text() === '同步')!
    await syncBtn.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('99')
    expect(wrapper.text()).toContain('42')
    expect(wrapper.text()).toContain('✓ 已同步')
  })

  it('shows error feedback on sync failure', async () => {
    mockHttp.post.mockRejectedValue(new Error('Network error'))

    const wrapper = mount(RepositoriesPage)
    await flushPromises()

    const syncBtn = wrapper.findAll('button').find(b => b.text() === '同步')!
    await syncBtn.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('同步失败')
  })

  it('disables sync button while syncing', async () => {
    let resolveSync: (value: unknown) => void
    mockHttp.post.mockReturnValue(new Promise(r => { resolveSync = r }))

    const wrapper = mount(RepositoriesPage)
    await flushPromises()

    const syncBtn = wrapper.findAll('button').find(b => b.text() === '同步')!
    await syncBtn.trigger('click')
    await flushPromises()

    const disabledBtn = wrapper.findAll('button').find(b => b.text().includes('同步中'))
    expect(disabledBtn!.attributes('disabled')).toBeDefined()

    resolveSync!({ data: mockRepo })
    await flushPromises()
  })
})
