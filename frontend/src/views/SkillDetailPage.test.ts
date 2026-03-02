import { describe, it, expect, vi, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import SkillDetailPage from './SkillDetailPage.vue'

vi.mock('@/api/http', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

vi.mock('marked', () => ({
  marked: (md: string) => `<p>${md}</p>`,
}))

import http from '@/api/http'

const mockHttp = vi.mocked(http)

const mockSkill = {
  id: 's1',
  name: 'My Agent Skill',
  description: 'A powerful agent skill for NLP tasks',
  readmeContent: '# Hello\nThis is a readme',
  author: 'testuser',
  downloadCount: 42,
  starCount: 128,
  forkCount: 15,
  tags: ['nlp', 'chat', 'agent'],
  repoUrl: 'https://github.com/testuser/my-repo',
  folderPath: 'skills/my-agent-skill',
  skillGroup: { id: 'g1', name: 'my-repo', description: 'A repo group' },
  createdAt: '2024-01-01T00:00:00',
  updatedAt: '2024-06-01T00:00:00',
}

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/skills/:id', component: SkillDetailPage },
      { path: '/skill-groups/:id', component: { template: '<div />' } },
    ],
  })
}

describe('SkillDetailPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('fetches and displays skill detail on mount', async () => {
    mockHttp.get.mockResolvedValue({ data: mockSkill })

    const router = createTestRouter()
    await router.push('/skills/s1')
    await router.isReady()

    const wrapper = mount(SkillDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    expect(mockHttp.get).toHaveBeenCalledWith('/skills/s1')
    expect(wrapper.text()).toContain('My Agent Skill')
    expect(wrapper.text()).toContain('A powerful agent skill for NLP tasks')
    expect(wrapper.text()).toContain('testuser')
    expect(wrapper.text()).toContain('128')
    expect(wrapper.text()).toContain('15')
  })

  it('renders tags as chips', async () => {
    mockHttp.get.mockResolvedValue({ data: mockSkill })

    const router = createTestRouter()
    await router.push('/skills/s1')
    await router.isReady()

    const wrapper = mount(SkillDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.text()).toContain('nlp')
    expect(wrapper.text()).toContain('chat')
    expect(wrapper.text()).toContain('agent')
  })

  it('renders skill group link', async () => {
    mockHttp.get.mockResolvedValue({ data: mockSkill })

    const router = createTestRouter()
    await router.push('/skills/s1')
    await router.isReady()

    const wrapper = mount(SkillDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    const groupLink = wrapper.find('a[href="/skill-groups/g1"]')
    expect(groupLink.exists()).toBe(true)
    expect(groupLink.text()).toBe('my-repo')
  })

  it('renders GitHub repo as external link', async () => {
    mockHttp.get.mockResolvedValue({ data: mockSkill })

    const router = createTestRouter()
    await router.push('/skills/s1')
    await router.isReady()

    const wrapper = mount(SkillDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    const repoLink = wrapper.find('a[href="https://github.com/testuser/my-repo"]')
    expect(repoLink.exists()).toBe(true)
    expect(repoLink.attributes('target')).toBe('_blank')
    expect(repoLink.attributes('rel')).toContain('noopener')
  })

  it('renders README content as HTML via v-html', async () => {
    mockHttp.get.mockResolvedValue({ data: mockSkill })

    const router = createTestRouter()
    await router.push('/skills/s1')
    await router.isReady()

    const wrapper = mount(SkillDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    const readmeSection = wrapper.find('.prose')
    expect(readmeSection.exists()).toBe(true)
    expect(readmeSection.text()).toContain('# Hello')
    expect(readmeSection.text()).toContain('This is a readme')
  })

  it('shows loading state initially', async () => {
    mockHttp.get.mockReturnValue(new Promise(() => {})) // never resolves

    const router = createTestRouter()
    await router.push('/skills/s1')
    await router.isReady()

    const wrapper = mount(SkillDetailPage, { global: { plugins: [router] } })

    expect(wrapper.text()).toContain('加载中...')
  })

  it('shows error state when skill not found (404)', async () => {
    mockHttp.get.mockRejectedValue({ response: { status: 404 } })

    const router = createTestRouter()
    await router.push('/skills/s1')
    await router.isReady()

    const wrapper = mount(SkillDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.text()).toContain('未找到该 Skill')
  })

  it('shows generic error state on network failure', async () => {
    mockHttp.get.mockRejectedValue(new Error('Network Error'))

    const router = createTestRouter()
    await router.push('/skills/s1')
    await router.isReady()

    const wrapper = mount(SkillDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.text()).toContain('加载失败，请稍后重试')
  })

  it('displays download command generated from skill metadata', async () => {
    mockHttp.get.mockResolvedValue({ data: mockSkill })

    const router = createTestRouter()
    await router.push('/skills/s1')
    await router.isReady()

    const wrapper = mount(SkillDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    const expectedCmd = 'git clone --filter=blob:none --sparse --depth=1 https://github.com/testuser/my-repo && cd my-repo && git sparse-checkout set skills/my-agent-skill'
    expect(wrapper.text()).toContain(expectedCmd)
    expect(wrapper.text()).toContain('下载命令')
  })

  it('copies download command and shows 已复制 feedback', async () => {
    mockHttp.get.mockResolvedValue({ data: mockSkill })
    mockHttp.post.mockResolvedValue({ data: {} })

    const writeTextMock = vi.fn().mockResolvedValue(undefined)
    Object.assign(navigator, { clipboard: { writeText: writeTextMock } })

    const router = createTestRouter()
    await router.push('/skills/s1')
    await router.isReady()

    const wrapper = mount(SkillDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    const copyBtn = wrapper.findAll('button').find(b => b.text() === '复制')
    expect(copyBtn).toBeDefined()

    await copyBtn!.trigger('click')
    await flushPromises()

    expect(writeTextMock).toHaveBeenCalledWith(
      'git clone --filter=blob:none --sparse --depth=1 https://github.com/testuser/my-repo && cd my-repo && git sparse-checkout set skills/my-agent-skill'
    )
    expect(wrapper.text()).toContain('已复制')
  })

  it('calls copy-event API when copy button is clicked', async () => {
    mockHttp.get.mockResolvedValue({ data: mockSkill })
    mockHttp.post.mockResolvedValue({ data: {} })

    Object.assign(navigator, { clipboard: { writeText: vi.fn().mockResolvedValue(undefined) } })

    const router = createTestRouter()
    await router.push('/skills/s1')
    await router.isReady()

    const wrapper = mount(SkillDetailPage, { global: { plugins: [router] } })
    await flushPromises()

    const copyBtn = wrapper.findAll('button').find(b => b.text() === '复制')
    await copyBtn!.trigger('click')
    await flushPromises()

    expect(mockHttp.post).toHaveBeenCalledWith('/skills/s1/copy-event')
  })
})
