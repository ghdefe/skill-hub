<script setup lang="ts">
import { ref, onMounted } from 'vue'
import http from '@/api/http'

interface SkillGroupSummary {
  id: string
  name: string
  skillCount: number
}

interface RepositoryItem {
  id: string
  githubOwner: string
  githubRepo: string
  url: string
  starCount: number
  forkCount: number
  scanPath: string
  lastSyncedAt: string | null
  createdAt: string
  skillGroup: SkillGroupSummary | null
}

const repoUrl = ref('')
const scanPath = ref('skills')
const importing = ref(false)
const successMessage = ref('')
const errorMessage = ref('')
const repositories = ref<RepositoryItem[]>([])
const loading = ref(false)
const deleteConfirmId = ref<string | null>(null)
const deleting = ref(false)
const syncingIds = ref<Set<string>>(new Set())
const syncSuccessId = ref<string | null>(null)
const syncErrorId = ref<string | null>(null)

async function fetchRepositories() {
  loading.value = true
  try {
    const { data } = await http.get<RepositoryItem[]>('/repositories')
    repositories.value = data
  } catch {
    // silently fail on list load
  } finally {
    loading.value = false
  }
}

async function importRepo() {
  successMessage.value = ''
  errorMessage.value = ''

  const url = repoUrl.value.trim()
  if (!url) {
    errorMessage.value = '请输入仓库 URL'
    return
  }

  const githubUrlPattern = /^https:\/\/github\.com\/[^/]+\/[^/]+/
  if (!githubUrlPattern.test(url)) {
    errorMessage.value = '仓库 URL 格式无效，请输入 https://github.com/owner/repo 格式的地址'
    return
  }

  importing.value = true
  try {
    await http.post('/repositories', { url, scanPath: scanPath.value.trim() || 'skills' }, { timeout: 120000 })
    successMessage.value = '仓库导入成功！'
    repoUrl.value = ''
    await fetchRepositories()
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { error?: { message?: string } } }, code?: string }
    if (axiosErr.code === 'ECONNABORTED') {
      errorMessage.value = '导入超时，仓库可能包含较多 Skills，请稍后在列表中查看或重试'
    } else {
      errorMessage.value =
        axiosErr.response?.data?.error?.message || '导入失败，请稍后重试'
    }
  } finally {
    importing.value = false
  }
}

async function deleteRepo(id: string) {
  deleting.value = true
  try {
    await http.delete(`/repositories/${id}`)
    deleteConfirmId.value = null
    await fetchRepositories()
  } catch {
    errorMessage.value = '删除失败，请稍后重试'
  } finally {
    deleting.value = false
  }
}

function formatTime(dateStr: string | null): string {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

async function syncRepo(id: string) {
  syncSuccessId.value = null
  syncErrorId.value = null
  syncingIds.value.add(id)
  try {
    const { data } = await http.post<RepositoryItem>(`/repositories/${id}/sync`, null, { timeout: 120000 })
    const idx = repositories.value.findIndex(r => r.id === id)
    if (idx !== -1) {
      repositories.value[idx] = data
    }
    syncSuccessId.value = id
  } catch {
    syncErrorId.value = id
  } finally {
    syncingIds.value.delete(id)
  }
}

onMounted(fetchRepositories)
</script>

<template>
  <div>
    <h1 class="text-2xl font-bold text-gray-900 mb-6">仓库管理</h1>

    <!-- Import Form -->
    <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-6 mb-8">
      <h2 class="text-lg font-semibold text-gray-800 mb-4">导入 GitHub 仓库</h2>
      <form class="space-y-3" @submit.prevent="importRepo">
        <div class="flex gap-3">
          <input
            v-model="repoUrl"
            type="text"
            placeholder="https://github.com/owner/repo"
            class="flex-1 border border-gray-300 rounded-md px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gray-900 focus:border-transparent"
            :disabled="importing"
          />
          <button
            type="submit"
            class="bg-gray-900 text-white px-5 py-2 rounded-md text-sm hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
            :disabled="importing"
          >
            <svg
              v-if="importing"
              class="animate-spin h-4 w-4"
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
            >
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
            {{ importing ? '正在导入...' : '导入仓库' }}
          </button>
        </div>
        <div v-if="importing" class="text-xs text-gray-400">正在扫描仓库中的 Skills，可能需要一些时间，请耐心等待...</div>
        <div class="flex items-center gap-2">
          <label class="text-xs text-gray-500 shrink-0">扫描目录:</label>
          <input
            v-model="scanPath"
            type="text"
            placeholder="skills"
            class="w-48 border border-gray-300 rounded-md px-3 py-1.5 text-xs focus:outline-none focus:ring-2 focus:ring-gray-900 focus:border-transparent"
            :disabled="importing"
          />
          <span class="text-xs text-gray-400">递归扫描包含 SKILL.md 的目录</span>
        </div>
      </form>

      <!-- Success Message -->
      <div
        v-if="successMessage"
        class="mt-3 p-3 bg-green-50 border border-green-200 rounded-md text-sm text-green-700"
      >
        {{ successMessage }}
      </div>

      <!-- Error Message -->
      <div
        v-if="errorMessage"
        class="mt-3 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-700"
      >
        {{ errorMessage }}
      </div>
    </div>

    <!-- Repository List -->
    <div class="bg-white rounded-lg shadow-sm border border-gray-200">
      <div class="px-6 py-4 border-b border-gray-200">
        <h2 class="text-lg font-semibold text-gray-800">已导入仓库</h2>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="p-8 text-center text-gray-500">
        加载中...
      </div>

      <!-- Empty State -->
      <div v-else-if="repositories.length === 0" class="p-8 text-center text-gray-400">
        暂无已导入的仓库，请在上方输入 GitHub 仓库 URL 进行导入。
      </div>

      <!-- List -->
      <ul v-else class="divide-y divide-gray-100">
        <li
          v-for="repo in repositories"
          :key="repo.id"
          class="px-6 py-4 flex items-center justify-between"
        >
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2 mb-1">
              <a
                :href="repo.url"
                target="_blank"
                rel="noopener noreferrer"
                class="text-sm font-medium text-gray-900 hover:underline truncate"
              >
                {{ repo.githubOwner }}/{{ repo.githubRepo }}
              </a>
            </div>
            <div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-gray-500">
              <span class="flex items-center gap-1" title="Star 数">
                <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                </svg>
                {{ repo.starCount }}
              </span>
              <span class="flex items-center gap-1" title="Fork 数">
                <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M7.707 3.293a1 1 0 010 1.414L5.414 7H11a7 7 0 017 7v2a1 1 0 11-2 0v-2a5 5 0 00-5-5H5.414l2.293 2.293a1 1 0 11-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd" />
                </svg>
                {{ repo.forkCount }}
              </span>
              <span v-if="repo.skillGroup">
                Skills 组: {{ repo.skillGroup.name }}
              </span>
              <span v-if="repo.skillGroup">
                活跃 Skills: {{ repo.skillGroup.skillCount }}
              </span>
              <span v-if="repo.scanPath && repo.scanPath !== 'skills'">
                扫描目录: {{ repo.scanPath }}
              </span>
              <span>
                最后同步: {{ formatTime(repo.lastSyncedAt) }}
              </span>
            </div>
          </div>
          <div class="ml-4 flex-shrink-0 flex items-center gap-3">
            <!-- Sync Button -->
            <div class="flex items-center gap-1">
              <button
                class="text-sm text-blue-600 hover:text-blue-800 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
                :disabled="syncingIds.has(repo.id)"
                @click="syncRepo(repo.id)"
              >
                <svg
                  v-if="syncingIds.has(repo.id)"
                  class="animate-spin h-3.5 w-3.5"
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
                {{ syncingIds.has(repo.id) ? '同步中...' : '同步' }}
              </button>
              <span v-if="syncingIds.has(repo.id)" class="text-xs text-gray-400">扫描中，请稍候</span>
              <span v-else-if="syncSuccessId === repo.id" class="text-xs text-green-600">✓ 已同步</span>
              <span v-if="syncErrorId === repo.id" class="text-xs text-red-600">同步失败</span>
            </div>

            <!-- Delete Button -->
            <button
              v-if="deleteConfirmId !== repo.id"
              class="text-sm text-red-600 hover:text-red-800"
              @click="deleteConfirmId = repo.id"
            >
              删除
            </button>
            <div v-else class="flex items-center gap-2">
              <button
                class="text-sm text-red-600 hover:text-red-800 font-medium disabled:opacity-50"
                :disabled="deleting"
                @click="deleteRepo(repo.id)"
              >
                确认删除
              </button>
              <button
                class="text-sm text-gray-500 hover:text-gray-700"
                :disabled="deleting"
                @click="deleteConfirmId = null"
              >
                取消
              </button>
            </div>
          </div>
        </li>
      </ul>
    </div>
  </div>
</template>
