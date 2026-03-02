<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '@/api/http'
import { generateSkillCommand, generateGroupCommand } from '@/utils/downloadCommand'
import { useAuth } from '@/composables/useAuth'

interface SkillSummary {
  id: string
  name: string
  description: string
  downloadCount: number
  status: string
  folderPath: string
}

interface SkillGroupDetail {
  id: string
  name: string
  description: string | null
  author: string | null
  repoUrl: string | null
  downloadCount: number
  totalSkillDownloads: number
  skills: SkillSummary[]
  createdAt: string
  updatedAt: string
}

const route = useRoute()
const router = useRouter()
const groupId = route.params.id as string

const { isLoggedIn, username } = useAuth()

const group = ref<SkillGroupDetail | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const batchCopied = ref(false)
const skillCopiedMap = ref<Record<string, boolean>>({})

// Inline editing state
const editing = ref(false)
const editName = ref('')
const editDescription = ref('')
const saving = ref(false)
const editError = ref<string | null>(null)

const isOwner = computed(() => {
  return isLoggedIn.value && group.value?.author === username.value
})

const repoName = computed(() => {
  if (!group.value?.repoUrl) return ''
  return group.value.repoUrl.split('/').pop() || ''
})

const activeSkills = computed(() => {
  if (!group.value) return []
  return group.value.skills.filter(s => s.status === 'ACTIVE')
})

const batchCommand = computed(() => {
  if (!group.value?.repoUrl || activeSkills.value.length === 0) return ''
  const paths = activeSkills.value.map(s => s.folderPath)
  return generateGroupCommand(group.value.repoUrl, repoName.value, paths)
})

function skillCommand(skill: SkillSummary): string {
  if (!group.value?.repoUrl) return ''
  return generateSkillCommand(group.value.repoUrl, repoName.value, skill.folderPath)
}

async function fetchGroup() {
  loading.value = true
  error.value = null
  try {
    const { data } = await http.get<SkillGroupDetail>(`/skill-groups/${groupId}`)
    group.value = data
  } catch (e: any) {
    if (e.response?.status === 404) {
      error.value = '未找到该 Skill 组'
    } else {
      error.value = '加载失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}

async function copyBatchCommand() {
  if (!batchCommand.value) return
  try {
    await navigator.clipboard.writeText(batchCommand.value)
    batchCopied.value = true
    setTimeout(() => { batchCopied.value = false }, 2000)
    http.post(`/skill-groups/${groupId}/copy-event`).catch(() => {})
  } catch {
    // Clipboard API may fail
  }
}

async function copySkillCommand(skill: SkillSummary) {
  const cmd = skillCommand(skill)
  if (!cmd) return
  try {
    await navigator.clipboard.writeText(cmd)
    skillCopiedMap.value[skill.id] = true
    setTimeout(() => { skillCopiedMap.value[skill.id] = false }, 2000)
    http.post(`/skills/${skill.id}/copy-event`).catch(() => {})
  } catch {
    // Clipboard API may fail
  }
}

function startEditing() {
  if (!group.value) return
  editName.value = group.value.name
  editDescription.value = group.value.description || ''
  editError.value = null
  editing.value = true
}

function cancelEditing() {
  editing.value = false
  editError.value = null
}

async function saveEditing() {
  if (!group.value) return
  saving.value = true
  editError.value = null
  try {
    await http.patch(`/skill-groups/${groupId}`, {
      name: editName.value,
      description: editDescription.value,
    })
    group.value.name = editName.value
    group.value.description = editDescription.value || null
    editing.value = false
  } catch (e: any) {
    if (e.response?.status === 409) {
      editError.value = '名称已存在，请使用其他名称'
    } else {
      editError.value = '保存失败，请稍后重试'
    }
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push('/')
}

onMounted(fetchGroup)
</script>

<template>
  <div>
    <!-- Back button -->
    <button
      class="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-900 mb-6 transition-colors"
      @click="goBack"
    >
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
      </svg>
      返回列表
    </button>

    <!-- Loading State -->
    <div v-if="loading" class="py-16 text-center text-gray-500">
      <svg class="animate-spin h-8 w-8 mx-auto mb-3 text-gray-400" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
      加载中...
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="py-16 text-center">
      <svg class="w-12 h-12 mx-auto mb-3 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      <p class="text-gray-500 mb-4">{{ error }}</p>
      <button
        class="px-4 py-2 text-sm bg-gray-900 text-white rounded-lg hover:bg-gray-800 transition-colors"
        @click="goBack"
      >
        返回首页
      </button>
    </div>

    <!-- Group Detail -->
    <div v-else-if="group">
      <!-- Header -->
      <div class="mb-8">
        <!-- View mode -->
        <div v-if="!editing">
          <div class="flex items-center gap-3 mb-2">
            <h1 class="text-3xl font-bold text-gray-900">{{ group.name }}</h1>
            <button
              v-if="isOwner"
              data-testid="edit-btn"
              class="px-3 py-1 text-xs font-medium text-gray-600 bg-gray-100 rounded-md hover:bg-gray-200 transition-colors"
              @click="startEditing"
            >
              编辑
            </button>
          </div>
          <p v-if="group.description" class="text-gray-500 text-base leading-relaxed">{{ group.description }}</p>
        </div>

        <!-- Edit mode -->
        <div v-else>
          <div class="space-y-3">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">名称</label>
              <input
                v-model="editName"
                data-testid="edit-name-input"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-gray-900 focus:border-transparent"
                placeholder="Skill 组名称"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">描述</label>
              <textarea
                v-model="editDescription"
                data-testid="edit-desc-input"
                rows="3"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-gray-900 focus:border-transparent resize-none"
                placeholder="Skill 组描述"
              />
            </div>
            <div v-if="editError" data-testid="edit-error" class="text-sm text-red-600">{{ editError }}</div>
            <div class="flex items-center gap-2">
              <button
                data-testid="save-btn"
                class="px-4 py-1.5 text-sm font-medium bg-gray-900 text-white rounded-lg hover:bg-gray-800 transition-colors disabled:opacity-50"
                :disabled="saving"
                @click="saveEditing"
              >
                {{ saving ? '保存中...' : '保存' }}
              </button>
              <button
                data-testid="cancel-btn"
                class="px-4 py-1.5 text-sm font-medium text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
                :disabled="saving"
                @click="cancelEditing"
              >
                取消
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Meta Info -->
      <div class="bg-white rounded-lg border border-gray-200 p-5 mb-6">
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
          <!-- Author -->
          <div v-if="group.author" class="flex items-center gap-2">
            <svg class="w-4 h-4 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd" />
            </svg>
            <span class="text-gray-500">作者:</span>
            <span class="text-gray-900">{{ group.author }}</span>
          </div>

          <!-- Total Downloads -->
          <div class="flex items-center gap-2">
            <svg class="w-4 h-4 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clip-rule="evenodd" />
            </svg>
            <span class="text-gray-500">总下载次数:</span>
            <span class="text-gray-900">{{ group.totalSkillDownloads }}</span>
          </div>

          <!-- Skill Count -->
          <div class="flex items-center gap-2">
            <svg class="w-4 h-4 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
              <path d="M7 3a1 1 0 000 2h6a1 1 0 100-2H7zM4 7a1 1 0 011-1h10a1 1 0 110 2H5a1 1 0 01-1-1zM2 11a2 2 0 012-2h12a2 2 0 012 2v4a2 2 0 01-2 2H4a2 2 0 01-2-2v-4z" />
            </svg>
            <span class="text-gray-500">Skills 数量:</span>
            <span class="text-gray-900">{{ activeSkills.length }}</span>
          </div>

          <!-- GitHub Repo Link -->
          <div v-if="group.repoUrl" class="flex items-center gap-2 sm:col-span-2">
            <svg class="w-4 h-4 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M12.316 3.051a1 1 0 01.633 1.265l-4 12a1 1 0 11-1.898-.632l4-12a1 1 0 011.265-.633zM5.707 6.293a1 1 0 010 1.414L3.414 10l2.293 2.293a1 1 0 11-1.414 1.414l-3-3a1 1 0 010-1.414l3-3a1 1 0 011.414 0zm8.586 0a1 1 0 011.414 0l3 3a1 1 0 010 1.414l-3 3a1 1 0 11-1.414-1.414L16.586 10l-2.293-2.293a1 1 0 010-1.414z" clip-rule="evenodd" />
            </svg>
            <span class="text-gray-500">仓库:</span>
            <a
              :href="group.repoUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="text-gray-900 hover:underline flex items-center gap-1"
            >
              {{ group.repoUrl }}
              <svg class="w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
              </svg>
            </a>
          </div>
        </div>
      </div>

      <!-- Batch Download Command -->
      <div v-if="batchCommand" class="mb-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-3">批量下载命令</h2>
        <div class="bg-gray-50 rounded-lg border border-gray-200 p-4">
          <div class="flex items-start justify-between gap-3">
            <code class="text-sm text-gray-800 break-all flex-1 font-mono">{{ batchCommand }}</code>
            <button
              class="shrink-0 px-3 py-1.5 text-xs font-medium rounded-md transition-colors"
              :class="batchCopied ? 'bg-green-100 text-green-700' : 'bg-gray-900 text-white hover:bg-gray-800'"
              data-testid="batch-copy-btn"
              @click="copyBatchCommand"
            >
              {{ batchCopied ? '已复制' : '复制' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Skills List -->
      <div class="mb-8">
        <h2 class="text-lg font-semibold text-gray-900 mb-3">包含的 Skills</h2>
        <div v-if="group.skills.length === 0" class="text-gray-500 text-sm">暂无 Skills</div>
        <div v-else class="space-y-4">
          <div
            v-for="s in group.skills"
            :key="s.id"
            class="bg-white rounded-lg border border-gray-200 p-4"
          >
            <div class="flex items-start justify-between mb-2">
              <div>
                <router-link
                  :to="`/skills/${s.id}`"
                  class="text-base font-medium text-gray-900 hover:underline"
                >
                  {{ s.name }}
                </router-link>
                <span
                  v-if="s.status === 'REMOVED'"
                  class="ml-2 px-2 py-0.5 text-xs bg-red-100 text-red-700 rounded-full"
                >
                  已移除
                </span>
              </div>
              <span class="text-xs text-gray-400">下载 {{ s.downloadCount }}</span>
            </div>
            <p v-if="s.description" class="text-sm text-gray-500 mb-3">{{ s.description }}</p>
            <!-- Individual Skill Download Command -->
            <div v-if="s.status === 'ACTIVE' && group.repoUrl" class="bg-gray-50 rounded border border-gray-100 p-3">
              <div class="flex items-start justify-between gap-3">
                <code class="text-xs text-gray-700 break-all flex-1 font-mono">{{ skillCommand(s) }}</code>
                <button
                  class="shrink-0 px-2.5 py-1 text-xs font-medium rounded transition-colors"
                  :class="skillCopiedMap[s.id] ? 'bg-green-100 text-green-700' : 'bg-gray-200 text-gray-700 hover:bg-gray-300'"
                  :data-testid="`skill-copy-btn-${s.id}`"
                  @click="copySkillCommand(s)"
                >
                  {{ skillCopiedMap[s.id] ? '已复制' : '复制' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
