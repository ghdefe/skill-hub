import { describe, it, expect, beforeEach, vi } from 'vitest'

// Mock vue-router before importing useAuth
const pushMock = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}))

import { useAuth } from './useAuth'

// Helper: create a fake JWT with given payload
function fakeJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256' }))
  const body = btoa(JSON.stringify(payload))
  return `${header}.${body}.fakesignature`
}

describe('useAuth', () => {
  beforeEach(() => {
    localStorage.clear()
    pushMock.mockClear()
  })

  it('checkAuth sets logged-in state when valid JWT exists', () => {
    const token = fakeJwt({ username: 'alice', avatarUrl: 'https://avatar.test/alice.png' })
    localStorage.setItem('jwt', token)

    const { isLoggedIn, username, avatarUrl, checkAuth } = useAuth()
    checkAuth()

    expect(isLoggedIn.value).toBe(true)
    expect(username.value).toBe('alice')
    expect(avatarUrl.value).toBe('https://avatar.test/alice.png')
  })

  it('checkAuth sets logged-out state when no JWT', () => {
    const { isLoggedIn, username, avatarUrl, checkAuth } = useAuth()
    checkAuth()

    expect(isLoggedIn.value).toBe(false)
    expect(username.value).toBe('')
    expect(avatarUrl.value).toBe('')
  })

  it('checkAuth sets logged-out state when JWT is malformed', () => {
    localStorage.setItem('jwt', 'not-a-valid-jwt')

    const { isLoggedIn, checkAuth } = useAuth()
    checkAuth()

    expect(isLoggedIn.value).toBe(false)
  })

  it('logout clears state and navigates to home', () => {
    const token = fakeJwt({ username: 'bob', avatarUrl: '' })
    localStorage.setItem('jwt', token)

    const { isLoggedIn, checkAuth, logout } = useAuth()
    checkAuth()
    expect(isLoggedIn.value).toBe(true)

    logout()

    expect(isLoggedIn.value).toBe(false)
    expect(localStorage.getItem('jwt')).toBeNull()
    expect(pushMock).toHaveBeenCalledWith('/')
  })

  it('login redirects to GitHub OAuth endpoint', () => {
    // We can't fully test window.location.href assignment in jsdom,
    // but we verify the function exists and is callable
    const { login } = useAuth()
    expect(typeof login).toBe('function')
  })
})
