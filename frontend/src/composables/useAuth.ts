import { ref } from 'vue'
import { useRouter } from 'vue-router'

const isLoggedIn = ref(false)
const username = ref('')
const avatarUrl = ref('')

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const base64 = token.split('.')[1]
    if (!base64) return null
    return JSON.parse(atob(base64))
  } catch {
    return null
  }
}

export function useAuth() {
  const router = useRouter()

  function checkAuth() {
    const token = localStorage.getItem('jwt')
    if (token) {
      const payload = decodeJwtPayload(token)
      if (payload) {
        isLoggedIn.value = true
        username.value = (payload.username as string) || ''
        avatarUrl.value = (payload.avatarUrl as string) || ''
        return
      }
    }
    isLoggedIn.value = false
    username.value = ''
    avatarUrl.value = ''
  }

  function login() {
    window.location.href = '/api/auth/github'
  }

  function logout() {
    localStorage.removeItem('jwt')
    isLoggedIn.value = false
    username.value = ''
    avatarUrl.value = ''
    router.push('/')
  }

  return {
    isLoggedIn,
    username,
    avatarUrl,
    checkAuth,
    login,
    logout,
  }
}
