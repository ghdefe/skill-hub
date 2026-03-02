import { describe, it, expect, vi, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import SkillGroupDetailPage from './SkillGroupDetailPage.vue'

vi.mock('@/api/http', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
  },
}))

vi.mock('@/composables/useAuth', () => {
  const isLoggedIn = { value: false }
  const username = { value: '' }
  return {
    useAuth: () => ({
      isLoggedIn,
      username,
      avatarUrl: { value: '' },
      checkAuth: vi.fn(),
      login: vi.fn(),
      logout: vi.fn(),
    }),
    __mockState: { isLoggedIn, username },
  }
})

import http from '@/api/http'
import { __mockState } from '@/composables/useAuth'

const mockHttp = vi.mocked(http)

const mockGroup = {
  id: 'g1',
  name: 'my-repo',
  description: 'A collection of agent skills',
  author: 'testuser',
  repoUrl: 'https://github.com/testuser/my-repo',
  downloadCount: 10,
  totalSkillDownloads: 55,
  skills: [
    { id: 's1', name: 'skill-a', description: 'First skill', downloadCount: 30, status: 'ACTIVE', folderPath: 'skills/skill-a' },
    { id: 's2', name: 'skill-b', description: 'Second skill', downloadCount: 25, status: 'ACTIVE', folderPath: 'skills/skill-b' },
    { id: 's3', name: 'skill-c', description: 'Removed skill', downloadCount: 0, status: 'REMOVED', folderPath: 'skills/skill-c' },
  ],
  createdAt: '2024-01-01T00:00:00',
  updatedAt: '2024-06-01T00:00:00',
}

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/skill-groups/:id', component: SkillGroupDetailPage },
      { path: '/skills/:id', component: { template: '<div />' } },
    ],
  })
}

describe('SkillGroupDetailPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    __mockState.isLoggedIn.value = false
    __mockState.username.value = ''
  })

  it('fetches and displays group detail on mount', async () => {
    mockHttp.get.mockResolvedValue({ data: mockGroup })

    const router = createTestRouter()
    await router.push('/skill-groups/g1')
    await router.isReady()

    const wrapper = mount(SkillGroupDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    expect(mockHttp.get).toHaveBeenCalledWith('/skill-groups/g1')
    expect(wrapper.text()).toContain('my-repo')
    expect(wrapper.text()).toContain('A collection of agent skills')
    expect(wrapper.text()).toContain('testuser')
    expect(wrapper.text()).toContain('55')
  })

  it('displays batch download command for all ACTIVE skills', async () => {
    mockHttp.get.mockResolvedValue({ data: mockGroup })

    const router = createTestRouter()
    await router.push('/skill-groups/g1')
    await router.isReady()

    const wrapper = mount(SkillGroupDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    const expectedCmd = 'git clone --filter=blob:none --sparse --depth=1 https://github.com/testuser/my-repo && cd my-repo && git sparse-checkout set skills/skill-a skills/skill-b'
    expect(wrapper.text()).toContain(expectedCmd)
    // Should NOT include removed skill path
    expect(wrapper.text()).not.toContain('git sparse-checkout set skills/skill-a skills/skill-b skills/skill-c')
  })

  it('displays individual skill download commands for ACTIVE skills', async () => {
    mockHttp.get.mockResolvedValue({ data: mockGroup })

    const router = createTestRouter()
    await router.push('/skill-groups/g1')
    await router.isReady()

    const wrapper = mount(SkillGroupDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.text()).toContain('git sparse-checkout set skills/skill-a')
    expect(wrapper.text()).toContain('git sparse-checkout set skills/skill-b')
  })

  it('shows REMOVED badge for removed skills', async () => {
    mockHttp.get.mockResolvedValue({ data: mockGroup })

    const router = createTestRouter()
    await router.push('/skill-groups/g1')
    await router.isReady()

    const wrapper = mount(SkillGroupDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.text()).toContain('已移除')
  })

  it('copies batch command and shows 已复制 feedback', async () => {
    mockHttp.get.mockResolvedValue({ data: mockGroup })
    mockHttp.post.mockResolvedValue({ data: {} })

    const writeTextMock = vi.fn().mockResolvedValue(undefined)
    Object.assign(navigator, { clipboard: { writeText: writeTextMock } })

    const router = createTestRouter()
    await router.push('/skill-groups/g1')
    await router.isReady()

    const wrapper = mount(SkillGroupDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    const batchCopyBtn = wrapper.find('[data-testid="batch-copy-btn"]')
    expect(batchCopyBtn.exists()).toBe(true)
    expect(batchCopyBtn.text()).toBe('复制')

    await batchCopyBtn.trigger('click')
    await flushPromises()

    expect(writeTextMock).toHaveBeenCalled()
    expect(batchCopyBtn.text()).toBe('已复制')
  })

  it('calls skill-groups copy-event API when batch copy is clicked', async () => {
    mockHttp.get.mockResolvedValue({ data: mockGroup })
    mockHttp.post.mockResolvedValue({ data: {} })

    Object.assign(navigator, { clipboard: { writeText: vi.fn().mockResolvedValue(undefined) } })

    const router = createTestRouter()
    await router.push('/skill-groups/g1')
    await router.isReady()

    const wrapper = mount(SkillGroupDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    await wrapper.find('[data-testid="batch-copy-btn"]').trigger('click')
    await flushPromises()

    expect(mockHttp.post).toHaveBeenCalledWith('/skill-groups/g1/copy-event')
  })

  it('copies individual skill command and calls skill copy-event API', async () => {
    mockHttp.get.mockResolvedValue({ data: mockGroup })
    mockHttp.post.mockResolvedValue({ data: {} })

    const writeTextMock = vi.fn().mockResolvedValue(undefined)
    Object.assign(navigator, { clipboard: { writeText: writeTextMock } })

    const router = createTestRouter()
    await router.push('/skill-groups/g1')
    await router.isReady()

    const wrapper = mount(SkillGroupDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    const skillCopyBtn = wrapper.find('[data-testid="skill-copy-btn-s1"]')
    expect(skillCopyBtn.exists()).toBe(true)

    await skillCopyBtn.trigger('click')
    await flushPromises()

    expect(writeTextMock).toHaveBeenCalledWith(
      'git clone --filter=blob:none --sparse --depth=1 https://github.com/testuser/my-repo && cd my-repo && git sparse-checkout set skills/skill-a'
    )
    expect(mockHttp.post).toHaveBeenCalledWith('/skills/s1/copy-event')
    expect(skillCopyBtn.text()).toBe('已复制')
  })

  it('shows loading state initially', async () => {
    mockHttp.get.mockReturnValue(new Promise(() => {}))

    const router = createTestRouter()
    await router.push('/skill-groups/g1')
    await router.isReady()

    const wrapper = mount(SkillGroupDetailPage, { global: { plugins: [router] } })

    expect(wrapper.text()).toContain('加载中...')
  })

  it('shows error state when group not found (404)', async () => {
    mockHttp.get.mockRejectedValue({ response: { status: 404 } })

    const router = createTestRouter()
    await router.push('/skill-groups/g1')
    await router.isReady()

    const wrapper = mount(SkillGroupDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.text()).toContain('未找到该 Skill 组')
  })

  it('renders GitHub repo as external link', async () => {
    mockHttp.get.mockResolvedValue({ data: mockGroup })

    const router = createTestRouter()
    await router.push('/skill-groups/g1')
    await router.isReady()

    const wrapper = mount(SkillGroupDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    const repoLink = wrapper.find('a[href="https://github.com/testuser/my-repo"]')
    expect(repoLink.exists()).toBe(true)
    expect(repoLink.attributes('target')).toBe('_blank')
  })

  it('renders skill links to detail pages', async () => {
    mockHttp.get.mockResolvedValue({ data: mockGroup })

    const router = createTestRouter()
    await router.push('/skill-groups/g1')
    await router.isReady()

    const wrapper = mount(SkillGroupDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    const skillLink = wrapper.find('a[href="/skills/s1"]')
    expect(skillLink.exists()).toBe(true)
    expect(skillLink.text()).toBe('skill-a')
  })

  // --- Inline Editing Tests ---

  it('does not show edit button when user is not logged in', async () => {
    mockHttp.get.mockResolvedValue({ data: mockGroup })

    const router = createTestRouter()
    await router.push('/skill-groups/g1')
    await router.isReady()

    const wrapper = mount(SkillGroupDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.find('[data-testid="edit-btn"]').exists()).toBe(false)
  })

  it('does not show edit button when logged-in user is not the owner', async () => {
    __mockState.isLoggedIn.value = true
    __mockState.username.value = 'otheruser'
    mockHttp.get.mockResolvedValue({ data: mockGroup })

    const router = createTestRouter()
    await router.push('/skill-groups/g1')
    await router.isReady()

    const wrapper = mount(SkillGroupDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.find('[data-testid="edit-btn"]').exists()).toBe(false)
  })

  it('shows edit button when logged-in user is the owner', async () => {
    __mockState.isLoggedIn.value = true
    __mockState.username.value = 'testuser'
    mockHttp.get.mockResolvedValue({ data: mockGroup })

    const router = createTestRouter()
    await router.push('/skill-groups/g1')
    await router.isReady()

    const wrapper = mount(SkillGroupDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.find('[data-testid="edit-btn"]').exists()).toBe(true)
  })

  it('enters edit mode with pre-filled values when edit button is clicked', async () => {
    __mockState.isLoggedIn.value = true
    __mockState.username.value = 'testuser'
    mockHttp.get.mockResolvedValue({ data: mockGroup })

    const router = createTestRouter()
    await router.push('/skill-groups/g1')
    await router.isReady()

    const wrapper = mount(SkillGroupDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    await wrapper.find('[data-testid="edit-btn"]').trigger('click')
    await flushPromises()

    const nameInput = wrapper.find('[data-testid="edit-name-input"]')
    const descInput = wrapper.find('[data-testid="edit-desc-input"]')
    expect(nameInput.exists()).toBe(true)
    expect(descInput.exists()).toBe(true)
    expect((nameInput.element as HTMLInputElement).value).toBe('my-repo')
    expect((descInput.element as HTMLTextAreaElement).value).toBe('A collection of agent skills')
  })

  it('cancels editing and returns to view mode', async () => {
    __mockState.isLoggedIn.value = true
    __mockState.username.value = 'testuser'
    mockHttp.get.mockResolvedValue({ data: mockGroup })

    const router = createTestRouter()
    await router.push('/skill-groups/g1')
    await router.isReady()

    const wrapper = mount(SkillGroupDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    await wrapper.find('[data-testid="edit-btn"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-testid="edit-name-input"]').exists()).toBe(true)

    await wrapper.find('[data-testid="cancel-btn"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="edit-name-input"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('my-repo')
  })

  it('saves edited name and description via PATCH API', async () => {
    __mockState.isLoggedIn.value = true
    __mockState.username.value = 'testuser'
    mockHttp.get.mockResolvedValue({ data: { ...mockGroup } })
    mockHttp.patch.mockResolvedValue({ data: {} })

    const router = createTestRouter()
    await router.push('/skill-groups/g1')
    await router.isReady()

    const wrapper = mount(SkillGroupDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    await wrapper.find('[data-testid="edit-btn"]').trigger('click')
    await flushPromises()

    await wrapper.find('[data-testid="edit-name-input"]').setValue('new-name')
    await wrapper.find('[data-testid="edit-desc-input"]').setValue('new description')
    await wrapper.find('[data-testid="save-btn"]').trigger('click')
    await flushPromises()

    expect(mockHttp.patch).toHaveBeenCalledWith('/skill-groups/g1', {
      name: 'new-name',
      description: 'new description',
    })
    // Should return to view mode with updated values
    expect(wrapper.find('[data-testid="edit-name-input"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('new-name')
  })

  it('shows error message on 409 conflict when saving', async () => {
    __mockState.isLoggedIn.value = true
    __mockState.username.value = 'testuser'
    mockHttp.get.mockResolvedValue({ data: { ...mockGroup } })
    mockHttp.patch.mockRejectedValue({ response: { status: 409 } })

    const router = createTestRouter()
    await router.push('/skill-groups/g1')
    await router.isReady()

    const wrapper = mount(SkillGroupDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    await wrapper.find('[data-testid="edit-btn"]').trigger('click')
    await flushPromises()

    await wrapper.find('[data-testid="edit-name-input"]').setValue('duplicate-name')
    await wrapper.find('[data-testid="save-btn"]').trigger('click')
    await flushPromises()

    const errorEl = wrapper.find('[data-testid="edit-error"]')
    expect(errorEl.exists()).toBe(true)
    expect(errorEl.text()).toContain('名称已存在')
  })

  it('shows generic error message on non-409 save failure', async () => {
    __mockState.isLoggedIn.value = true
    __mockState.username.value = 'testuser'
    mockHttp.get.mockResolvedValue({ data: { ...mockGroup } })
    mockHttp.patch.mockRejectedValue({ response: { status: 500 } })

    const router = createTestRouter()
    await router.push('/skill-groups/g1')
    await router.isReady()

    const wrapper = mount(SkillGroupDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    await wrapper.find('[data-testid="edit-btn"]').trigger('click')
    await flushPromises()

    await wrapper.find('[data-testid="save-btn"]').trigger('click')
    await flushPromises()

    const errorEl = wrapper.find('[data-testid="edit-error"]')
    expect(errorEl.exists()).toBe(true)
    expect(errorEl.text()).toContain('保存失败')
  })
})
