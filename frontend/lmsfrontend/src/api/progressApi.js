import { json, request } from './http.js'

// Mặc định gọi qua cùng origin (Vite proxy khi dev, Gateway khi deploy) để không vướng CORS.
const API_URL = import.meta.env.VITE_PROGRESS_API_URL ?? ''
const V1 = `${API_URL}/api/v1/progress`

let tokenProvider = async () => null

export function setProgressTokenProvider(provider) {
  tokenProvider = typeof provider === 'function' ? provider : async () => null
}

async function progressRequest(path, options = {}) {
  const token = await tokenProvider().catch(() => null)
  return request(`${V1}${path}`, { ...options, token })
}

/** UC-P04 — nộp bài quiz và xem lịch sử làm bài. */
export const progressApi = {
  submitQuiz: (courseId, quizId, payload) =>
    progressRequest(`/courses/${courseId}/quizzes/${quizId}/submit`, json('POST', payload)),
  quizAttempts: (courseId, quizId, studentId) =>
    progressRequest(
      `/courses/${courseId}/quizzes/${quizId}/attempts?studentId=${encodeURIComponent(studentId)}`,
    ),
}
