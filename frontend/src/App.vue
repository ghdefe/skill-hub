<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuth } from '@/composables/useAuth'

const { isLoggedIn, username, avatarUrl, checkAuth, login, logout } = useAuth()

onMounted(checkAuth)
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <nav class="bg-white shadow-sm border-b border-gray-200">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between h-16 items-center">
          <router-link to="/" class="text-xl font-bold text-gray-900">
            Agent Skills
          </router-link>
          <div class="flex items-center gap-4">
            <router-link
              v-if="isLoggedIn"
              to="/repositories"
              class="text-gray-600 hover:text-gray-900"
            >
              仓库管理
            </router-link>
            <template v-if="isLoggedIn">
              <img
                :src="avatarUrl"
                :alt="username"
                class="w-8 h-8 rounded-full"
              />
              <span class="text-sm text-gray-700">{{ username }}</span>
              <button
                class="text-sm text-gray-500 hover:text-gray-700"
                @click="logout"
              >
                退出
              </button>
            </template>
            <button
              v-else
              class="bg-gray-900 text-white px-4 py-2 rounded-md text-sm hover:bg-gray-700"
              @click="login"
            >
              GitHub 登录
            </button>
          </div>
        </div>
      </div>
    </nav>
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <router-view />
    </main>
  </div>
</template>
