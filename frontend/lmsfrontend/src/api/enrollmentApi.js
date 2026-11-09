import { json, request } from './http.js'

// Đăng ký khoá học phải qua API Gateway (service này tin header X-User-Id/X-User-Role do Gateway bơm).
const API_URL = import.meta.env.VITE_ENROLLMENT_API_URL ?? 'http://localhost:8080'
const V1 = `${API_URL}/api/v1/enrollments`

let tokenProvider = async () => null

export function setEnrollmentTokenProvider(provider) {
  tokenProvider = typeof provider === 'function' ? provider : async () => null
}

async function enrollmentRequest(path, options = {}) {
  const token = await tokenProvider().catch(() => null)
  return request(`${V1}${path}`, { ...options, token })
}

/** UC-E01 — học viên tự đăng ký khoá học. */
export const enrollmentApi = {
  enroll: (courseId) => enrollmentRequest('', json('POST', { courseId })),
  myEnrollments: () => enrollmentRequest('/me'),
}
