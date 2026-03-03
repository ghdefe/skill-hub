<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'

interface SkillItem {
  id: string
  name: string
  description: string
  author: string
  downloadCount: number
  starCount: number
  forkCount: number
  tags: string[]
  createdAt: string
}

interface PageResponse {
  items: SkillItem[]
  total: number
  page: number
  pageSize: number
}

interface TagItem {
  id: string
  name: string
}

interface SkillGroupItem {
  id: string
  name: string
  description: string
  author: string
  downloadCount: number
  skillCount: number
  starCount: number
  forkCount: number
  createdAt: string
}

const router = useRouter()

// Banner
const showBanner = ref(!localStorage.getItem('skillhub-banner-dismissed'))

function dismissBanner() {
  showBanner.value = false
  localStorage.setItem('skillhub-banner-dismissed', '1')
}

// View mode
type ViewMode = 'skills' | 'groups'
const viewMode = ref<ViewMode>('skills')

// Skills state
const skills = ref<SkillItem[]>([])
const total = ref(0)
const loading = ref(false)
const searchQuery = ref('')
const selectedTags = ref<string[]>([])
const sortField = ref('createdAt')
const sortOrder = ref<'asc' | 'desc'>('desc')
const currentPage = ref(1)
const pageSize = 20
const allTags = ref<TagItem[]>([])

// SkillGroups state
const skillGroups = ref<SkillGroupItem[]>([])
const groupsLoading = ref(false)

// Debounce timer
let debounceTimer: ReturnType<typeof setTimeout> | null = null

// Sort options
const sortOptions = [
  { value: 'downloads', label: '下载次数' },
  { value: 'createdAt', label: '发布时间' },
  { value: 'name', label: '名称' },
  { value: 'starCount', label: 'Star 数' },
]

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

const visiblePages = computed(() => {
  const pages: number[] = []
  const tp = totalPages.value
  const cp = currentPage.value
  const maxVisible = 5
  if (tp <= maxVisible) {
    for (let i = 1; i <= tp; i++) pages.push(i)
  } else {
    let start = Math.max(1, cp - 2)
    let end = Math.min(tp, start + maxVisible - 1)
    if (end - start < maxVisible - 1) {
      start = Math.max(1, end - maxVisible + 1)
    }
    for (let i = start; i <= end; i++) pages.push(i)
  }
  return pages
})

async function fetchSkills() {
  loading.value = true
  try {
    const params: Record<string, string | number> = {
      page: currentPage.value,
      pageSize,
      sort: sortField.value,
      order: sortOrder.value,
    }
    if (searchQuery.value.trim()) params.q = searchQuery.value.trim()
    if (selectedTags.value.length > 0) params.tags = selectedTags.value.join(',')
    const { data } = await http.get<PageResponse>('/skills', { params })
    skills.value = data.items
    total.value = data.total
  } catch {
    skills.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function fetchSkillGroups() {
  groupsLoading.value = true
  try {
    const { data } = await http.get<SkillGroupItem[]>('/skill-groups')
    skillGroups.value = data
  } catch {
    skillGroups.value = []
  } finally {
    groupsLoading.value = false
  }
}

async function fetchTags() {
  try {
    const { data } = await http.get<TagItem[]>('/tags')
    allTags.value = data
  } catch {
    allTags.value = []
  }
}

function switchView(mode: ViewMode) {
  if (viewMode.value === mode) return
  viewMode.value = mode
  if (mode === 'groups' && skillGroups.value.length === 0) {
    fetchSkillGroups()
  }
}

function onSearchInput() {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    currentPage.value = 1
    fetchSkills()
  }, 500)
}

function toggleTag(tagName: string) {
  const idx = selectedTags.value.indexOf(tagName)
  if (idx >= 0) selectedTags.value.splice(idx, 1)
  else selectedTags.value.push(tagName)
  currentPage.value = 1
  fetchSkills()
}

function onSortChange() {
  currentPage.value = 1
  fetchSkills()
}

function toggleSortOrder() {
  sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  currentPage.value = 1
  fetchSkills()
}

function goToPage(page: number) {
  if (page < 1 || page > totalPages.value || page === currentPage.value) return
  currentPage.value = page
  fetchSkills()
}

function goToSkill(id: string) {
  router.push(`/skills/${id}`)
}

function goToGroup(id: string) {
  router.push(`/skill-groups/${id}`)
}

function truncate(text: string | null, maxLen: number): string {
  if (!text) return ''
  return text.length > maxLen ? text.slice(0, maxLen) + '...' : text
}

onMounted(() => {
  fetchTags()
  fetchSkills()
})
</script>

<template>
  <div>
    <!-- Quick Start Banner -->
    <div v-if="showBanner" class="bg-gray-900 text-white rounded-lg p-4 mb-6 flex items-center justify-between gap-4">
      <div class="flex items-center gap-3 min-w-0">
        <span class="text-lg shrink-0">🔥</span>
        <div class="min-w-0">
          <p class="text-sm font-medium">SkillHub — 一行命令获取 AI Agent 技能</p>
          <p class="text-xs text-gray-400 mt-0.5">只需 git，跨平台可用 · 深度结合 GitHub · 标准化格式一键导入</p>
        </div>
      </div>
      <div class="flex items-center gap-2 shrink-0">
        <router-link to="/about" class="px-3 py-1.5 text-xs font-medium bg-white text-gray-900 rounded-md hover:bg-gray-100 transition-colors">
          了解更多
        </router-link>
        <button class="p-1 text-gray-400 hover:text-white transition-colors" title="关闭" @click="dismissBanner">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
    </div>

    <!-- View Mode Tabs -->
    <div class="flex items-center gap-6 mb-6">
      <button
        class="text-2xl font-bold pb-1 border-b-2 transition-colors"
        :class="viewMode === 'skills' ? 'text-gray-900 border-gray-900' : 'text-gray-400 border-transparent hover:text-gray-600'"
        @click="switchView('skills')"
      >
        Skills
      </button>
      <button
        class="text-2xl font-bold pb-1 border-b-2 transition-colors"
        :class="viewMode === 'groups' ? 'text-gray-900 border-gray-900' : 'text-gray-400 border-transparent hover:text-gray-600'"
        @click="switchView('groups')"
      >
        Skill Groups
      </button>
    </div>

    <!-- ==================== Skills View ==================== -->
    <template v-if="viewMode === 'skills'">
      <!-- Search Bar -->
      <div class="mb-4">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索 Skills..."
          class="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-gray-900 focus:border-transparent"
          @input="onSearchInput"
        />
      </div>

      <!-- Tag Filter -->
      <div v-if="allTags.length > 0" class="mb-4 flex flex-wrap gap-2">
        <button
          v-for="tag in allTags"
          :key="tag.id"
          class="px-3 py-1 rounded-full text-xs font-medium border transition-colors"
          :class="selectedTags.includes(tag.name) ? 'bg-gray-900 text-white border-gray-900' : 'bg-white text-gray-600 border-gray-300 hover:border-gray-400'"
          @click="toggleTag(tag.name)"
        >
          {{ tag.name }}
        </button>
      </div>

      <!-- Sort Controls -->
      <div class="mb-6 flex items-center gap-3">
        <label class="text-sm text-gray-500">排序:</label>
        <select
          v-model="sortField"
          class="border border-gray-300 rounded-md px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-gray-900 focus:border-transparent"
          @change="onSortChange"
        >
          <option v-for="opt in sortOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
        <button
          class="p-1.5 rounded-md border border-gray-300 hover:bg-gray-100 transition-colors"
          :title="sortOrder === 'asc' ? '升序' : '降序'"
          @click="toggleSortOrder"
        >
          <svg class="w-4 h-4 text-gray-600 transition-transform" :class="{ 'rotate-180': sortOrder === 'asc' }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
        </button>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="py-16 text-center text-gray-500">
        <svg class="animate-spin h-8 w-8 mx-auto mb-3 text-gray-400" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
        加载中...
      </div>

      <!-- Empty -->
      <div v-else-if="skills.length === 0" class="py-16 text-center text-gray-400">
        <svg class="w-12 h-12 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <p>未找到匹配的 Skills</p>
      </div>

      <!-- Skill Cards -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
        <div
          v-for="skill in skills"
          :key="skill.id"
          class="bg-white rounded-lg border border-gray-200 p-5 hover:shadow-md transition-shadow cursor-pointer"
          @click="goToSkill(skill.id)"
        >
          <h3 class="text-base font-semibold text-gray-900 mb-1">{{ skill.name }}</h3>
          <p class="text-sm text-gray-500 mb-3 leading-relaxed">{{ truncate(skill.description, 100) }}</p>
          <div v-if="skill.tags.length > 0" class="flex flex-wrap gap-1.5 mb-3">
            <span v-for="tag in skill.tags" :key="tag" class="px-2 py-0.5 bg-gray-100 text-gray-600 rounded text-xs">{{ tag }}</span>
          </div>
          <div class="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-gray-400 border-t border-gray-100 pt-3">
            <span v-if="skill.author" class="flex items-center gap-1">
              <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd" /></svg>
              {{ skill.author }}
            </span>
            <span class="flex items-center gap-1" title="下载次数">
              <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clip-rule="evenodd" /></svg>
              {{ skill.downloadCount }}
            </span>
            <span class="flex items-center gap-1" title="Star 数">
              <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" /></svg>
              {{ skill.starCount }}
            </span>
            <span class="flex items-center gap-1" title="Fork 数">
              <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M7.707 3.293a1 1 0 010 1.414L5.414 7H11a7 7 0 017 7v2a1 1 0 11-2 0v-2a5 5 0 00-5-5H5.414l2.293 2.293a1 1 0 11-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd" /></svg>
              {{ skill.forkCount }}
            </span>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="!loading && totalPages > 1" class="flex items-center justify-center gap-1">
        <button class="px-3 py-1.5 text-sm rounded-md border border-gray-300 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">上一页</button>
        <button v-for="p in visiblePages" :key="p" class="px-3 py-1.5 text-sm rounded-md border transition-colors" :class="p === currentPage ? 'bg-gray-900 text-white border-gray-900' : 'border-gray-300 hover:bg-gray-100'" @click="goToPage(p)">{{ p }}</button>
        <button class="px-3 py-1.5 text-sm rounded-md border border-gray-300 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">下一页</button>
      </div>
    </template>

    <!-- ==================== Skill Groups View ==================== -->
    <template v-if="viewMode === 'groups'">
      <!-- Loading -->
      <div v-if="groupsLoading" class="py-16 text-center text-gray-500">
        <svg class="animate-spin h-8 w-8 mx-auto mb-3 text-gray-400" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
        加载中...
      </div>

      <!-- Empty -->
      <div v-else-if="skillGroups.length === 0" class="py-16 text-center text-gray-400">
        <svg class="w-12 h-12 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
        </svg>
        <p>暂无 Skill Groups</p>
      </div>

      <!-- Group Cards -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
          v-for="group in skillGroups"
          :key="group.id"
          class="bg-white rounded-lg border border-gray-200 p-5 hover:shadow-md transition-shadow cursor-pointer"
          @click="goToGroup(group.id)"
        >
          <h3 class="text-base font-semibold text-gray-900 mb-1">{{ group.name }}</h3>
          <p class="text-sm text-gray-500 mb-3 leading-relaxed">{{ truncate(group.description, 100) }}</p>
          <div class="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-gray-400 border-t border-gray-100 pt-3">
            <span v-if="group.author" class="flex items-center gap-1">
              <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd" /></svg>
              {{ group.author }}
            </span>
            <span class="flex items-center gap-1" title="Skills 数量">
              <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path d="M7 3a1 1 0 000 2h6a1 1 0 100-2H7zM4 7a1 1 0 011-1h10a1 1 0 110 2H5a1 1 0 01-1-1zM2 11a2 2 0 012-2h12a2 2 0 012 2v4a2 2 0 01-2 2H4a2 2 0 01-2-2v-4z" /></svg>
              {{ group.skillCount }} skills
            </span>
            <span class="flex items-center gap-1" title="下载次数">
              <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clip-rule="evenodd" /></svg>
              {{ group.downloadCount }}
            </span>
            <span class="flex items-center gap-1" title="Star 数">
              <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" /></svg>
              {{ group.starCount }}
            </span>
            <span class="flex items-center gap-1" title="Fork 数">
              <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M7.707 3.293a1 1 0 010 1.414L5.414 7H11a7 7 0 017 7v2a1 1 0 11-2 0v-2a5 5 0 00-5-5H5.414l2.293 2.293a1 1 0 11-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd" /></svg>
              {{ group.forkCount }}
            </span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
