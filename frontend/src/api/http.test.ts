import { describe, it, expect, beforeEach } from 'vitest'
import http from './http'

describe('http axios instance', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('should have baseURL set to /api', () => {
    expect(http.defaults.baseURL).toBe('/api')
  })

  it('should attach Authorization header when JWT exists in localStorage', async () => {
    const fakeToken = 'eyJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6InRlc3QifQ.abc123'
    localStorage.setItem('jwt', fakeToken)

    // Trigger the request interceptor by creating a config manually
    const interceptor = http.interceptors.request as unknown as {
      handlers: Array<{ fulfilled: (config: Record<string, unknown>) => Record<string, unknown> }>
    }
    const handler = interceptor.handlers[0]
    const config = { headers: {} as Record<string, string> }
    const result = handler.fulfilled(config) as { headers: Record<string, string> }

    expect(result.headers.Authorization).toBe(`Bearer ${fakeToken}`)
  })

  it('should not attach Authorization header when no JWT in localStorage', () => {
    const interceptor = http.interceptors.request as unknown as {
      handlers: Array<{ fulfilled: (config: Record<string, unknown>) => Record<string, unknown> }>
    }
    const handler = interceptor.handlers[0]
    const config = { headers: {} as Record<string, string> }
    const result = handler.fulfilled(config) as { headers: Record<string, string> }

    expect(result.headers.Authorization).toBeUndefined()
  })
})
