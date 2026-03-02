<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import axios from 'axios'
import http from '@/api/http'
import { generateSkillCommand } from '@/utils/downloadCommand'

interface SkillGroupInfo {
  id: string
  name: string
  description: string
}

interface SkillDetail {
  id: string
  name: string
  description: string
  readmeContent: string | null
  author: string | null
  downloadCount: number
  starCount: number
  forkCount: number
  tags: string[]
  repoUrl: string | null
  folderPath: string
  skillGroup: SkillGroupInfo | null
  skillMdUrl: string | null
  createdAt: string
  updatedAt: string
}

const route = useRoute()
const router = useRouter()
const skillId = route.params.id as string

const skill = ref<SkillDetail | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const copied = ref(false)
const skillMdContent = ref<string | null>(null)
const skillMdLoading = ref(false)

const renderedReadme = computed(() => {
  if (!skill.value?.readmeContent) return ''
  return marked(skill.value.readmeContent) as string
})

const downloadCommand = computed(() => {
  if (!skill.value?.repoUrl || !skill.value?.folderPath) return ''
  const repoName = skill.value.repoUrl.split('/').pop() || ''
  return generateSkillCommand(skill.value.repoUrl, repoName, skill.value.folderPath)
})

async function fetchSkill() {
  loading.value = true
  error.value = null
  try {
    const { data } = await http.get<SkillDetail>(`/skills/${skillId}`)
    skill.value = data
    if (data.skillMdUrl) {
      fetchSkillMd(data.skillMdUrl)
    }
  } catch (e: any) {
    if (e.response?.status === 404) {
      error.value = '未找到该 Skill'
    } else {
      error.value = '加载失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}

async function fetchSkillMd(url: string) {
  skillMdLoading.value = true
  try {
    const { data } = await axios.get<string>(url, { timeout: 10000 })
    skillMdContent.value = data
  } catch {
    // Silently fail — README is still available as fallback
    skillMdContent.value = null
  } finally {
    skillMdLoading.value = false
  }
}

async function copyCommand() {
  if (!downloadCommand.value) return
  try {
    await navigator.clipboard.writeText(downloadCommand.value)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
    // Fire-and-forget: record copy event
    http.post(`/skills/${skillId}/copy-event`).catch(() => {})
  } catch {
    // Clipboard API may fail in some environments
  }
}

function goBack() {
  router.push('/')
}

onMounted(fetchSkill)
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

    <!-- Skill Detail -->
    <div v-else-if="skill">
      <!-- Header -->
      <div class="mb-8">
        <h1 class="text-3xl font-bold text-gray-900 mb-2">{{ skill.name }}</h1>
        <p v-if="skill.description" class="text-gray-500 text-base leading-relaxed">{{ skill.description }}</p>
      </div>

      <!-- Meta Info -->
      <div class="bg-white rounded-lg border border-gray-200 p-5 mb-6">
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
          <!-- Author -->
          <div v-if="skill.author" class="flex items-center gap-2">
            <svg class="w-4 h-4 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd" />
            </svg>
            <span class="text-gray-500">作者:</span>
            <span class="text-gray-900">{{ skill.author }}</span>
          </div>

          <!-- Skill Group -->
          <div v-if="skill.skillGroup" class="flex items-center gap-2">
            <svg class="w-4 h-4 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
              <path d="M7 3a1 1 0 000 2h6a1 1 0 100-2H7zM4 7a1 1 0 011-1h10a1 1 0 110 2H5a1 1 0 01-1-1zM2 11a2 2 0 012-2h12a2 2 0 012 2v4a2 2 0 01-2 2H4a2 2 0 01-2-2v-4z" />
            </svg>
            <span class="text-gray-500">所属组:</span>
            <router-link
              :to="`/skill-groups/${skill.skillGroup.id}`"
              class="text-gray-900 hover:underline"
            >
              {{ skill.skillGroup.name }}
            </router-link>
          </div>

          <!-- Star Count -->
          <div class="flex items-center gap-2">
            <svg class="w-4 h-4 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
              <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
            </svg>
            <span class="text-gray-500">Star:</span>
            <span class="text-gray-900">{{ skill.starCount }}</span>
          </div>

          <!-- Fork Count -->
          <div class="flex items-center gap-2">
            <svg class="w-4 h-4 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M7.707 3.293a1 1 0 010 1.414L5.414 7H11a7 7 0 017 7v2a1 1 0 11-2 0v-2a5 5 0 00-5-5H5.414l2.293 2.293a1 1 0 11-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd" />
            </svg>
            <span class="text-gray-500">Fork:</span>
            <span class="text-gray-900">{{ skill.forkCount }}</span>
          </div>

          <!-- GitHub Repo Link -->
          <div v-if="skill.repoUrl" class="flex items-center gap-2 sm:col-span-2">
            <svg class="w-4 h-4 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M12.316 3.051a1 1 0 01.633 1.265l-4 12a1 1 0 11-1.898-.632l4-12a1 1 0 011.265-.633zM5.707 6.293a1 1 0 010 1.414L3.414 10l2.293 2.293a1 1 0 11-1.414 1.414l-3-3a1 1 0 010-1.414l3-3a1 1 0 011.414 0zm8.586 0a1 1 0 011.414 0l3 3a1 1 0 010 1.414l-3 3a1 1 0 11-1.414-1.414L16.586 10l-2.293-2.293a1 1 0 010-1.414z" clip-rule="evenodd" />
            </svg>
            <span class="text-gray-500">仓库:</span>
            <a
              :href="skill.repoUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="text-gray-900 hover:underline flex items-center gap-1"
            >
              {{ skill.repoUrl }}
              <svg class="w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
              </svg>
            </a>
          </div>
        </div>
      </div>

      <!-- Download Command -->
      <div v-if="downloadCommand" class="mb-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-3">下载命令</h2>
        <div class="bg-gray-50 rounded-lg border border-gray-200 p-4">
          <div class="flex items-start justify-between gap-3">
            <code class="text-sm text-gray-800 break-all flex-1 font-mono">{{ downloadCommand }}</code>
            <button
              class="shrink-0 px-3 py-1.5 text-xs font-medium rounded-md transition-colors"
              :class="copied ? 'bg-green-100 text-green-700' : 'bg-gray-900 text-white hover:bg-gray-800'"
              @click="copyCommand"
            >
              {{ copied ? '已复制' : '复制' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Tags -->
      <div v-if="skill.tags.length > 0" class="mb-6">
        <h2 class="text-sm font-medium text-gray-500 mb-2">标签</h2>
        <div class="flex flex-wrap gap-2">
          <span
            v-for="tag in skill.tags"
            :key="tag"
            class="px-3 py-1 bg-gray-100 text-gray-700 rounded-full text-xs font-medium"
          >
            {{ tag }}
          </span>
        </div>
      </div>

      <!-- SKILL.md Content -->
      <div v-if="skillMdLoading" class="mb-8">
        <h2 class="text-lg font-semibold text-gray-900 mb-3">Skill 详情</h2>
        <div class="bg-white rounded-lg border border-gray-200 p-6 text-center text-gray-400">
          <svg class="animate-spin h-5 w-5 mx-auto mb-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          加载中...
        </div>
      </div>
      <div v-else-if="skillMdContent" class="mb-8">
        <h2 class="text-lg font-semibold text-gray-900 mb-3">Skill 详情</h2>
        <pre class="bg-white rounded-lg border border-gray-200 p-6 text-sm text-gray-800 font-mono whitespace-pre-wrap break-words overflow-auto max-h-[600px]">{{ skillMdContent }}</pre>
      </div>

      <!-- README Content -->
      <div v-if="renderedReadme" class="mb-8">
        <h2 class="text-lg font-semibold text-gray-900 mb-3">README</h2>
        <div
          class="bg-white rounded-lg border border-gray-200 p-6 prose prose-sm max-w-none prose-headings:text-gray-900 prose-a:text-blue-600 prose-code:bg-gray-100 prose-code:px-1 prose-code:py-0.5 prose-code:rounded prose-pre:bg-gray-50 prose-pre:border prose-pre:border-gray-200"
          v-html="renderedReadme"
        />
      </div>
    </div>
  </div>
</template>
