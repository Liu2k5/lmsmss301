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

export const enrollmentApi = {
  /** UC-E01 — học viên tự đăng ký khoá học. */
  enroll: (courseId) => enrollmentRequest('', json('POST', { courseId })),

  /** UC-E02 — học viên huỷ đăng ký khoá học. */
  cancel: (courseId) => enrollmentRequest(`/${courseId}`, { method: 'DELETE' }),

  /** UC-E03 — học viên xem danh sách khoá học của mình. */
  myEnrollments: () => enrollmentRequest('/me'),

  /** UC-E04 — kiểm tra trạng thái đăng ký của học viên (nội bộ). */
  check: (studentId, courseId) =>
    enrollmentRequest(`/check?studentId=${encodeURIComponent(studentId)}&courseId=${encodeURIComponent(courseId)}`),

  /** UC-E05 — giảng viên / admin xem danh sách học viên đăng ký khoá học. */
  getCourseStudents: (courseId) => enrollmentRequest(`/courses/${courseId}/students`),

  /** UC-E06 — admin cấp quyền truy cập khoá học cho học viên. */
  grant: (studentId, courseId) => enrollmentRequest('/admin/grant', json('POST', { studentId, courseId })),

  /** UC-E07 — admin thu hồi quyền truy cập khoá học. */
  revoke: (studentId, courseId) => enrollmentRequest('/admin/revoke', json('POST', { studentId, courseId })),
}
