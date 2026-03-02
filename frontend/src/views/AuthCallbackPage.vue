<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const error = ref('')
const loading = ref(true)

function isValidJwt(token: string): boolean {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return false
    JSON.parse(atob(parts[1]))
    return true
  } catch {
    return false
  }
}

onMounted(() => {
  const token = route.query.token as string | undefined
  const errorParam = route.query.error as string | undefined

  if (errorParam) {
    error.value = errorParam
    loading.value = false
    return
  }

  if (!token) {
    error.value = '未收到认证令牌，请重新登录'
    loading.value = false
    return
  }

  if (!isValidJwt(token)) {
    error.value = '认证令牌无效，请重新登录'
    loading.value = false
    return
  }

  localStorage.setItem('jwt', token)
  router.push('/')
})

function retry() {
  window.location.href = '/api/auth/github'
}
</script>

<template>
  <div class="flex flex-col items-center justify-center py-20">
    <template v-if="error">
      <div class="bg-red-50 border border-red-200 rounded-lg p-6 text-center max-w-md">
        <svg class="mx-auto h-10 w-10 text-red-400 mb-3" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9 3.75h.008v.008H12v-.008Z" />
        </svg>
        <p class="text-red-700 mb-4">{{ error }}</p>
        <button
          class="bg-gray-900 text-white px-4 py-2 rounded-md text-sm hover:bg-gray-700"
          @click="retry"
        >
          重新登录
        </button>
      </div>
    </template>
    <template v-else-if="loading">
      <div class="text-center">
        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900 mx-auto mb-3"></div>
        <p class="text-gray-500">正在登录...</p>
      </div>
    </template>
  </div>
</template>
