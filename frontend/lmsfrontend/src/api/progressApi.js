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
  /** UC-P05 — tiến độ học của học viên trong khoá (dùng để biết đã hoàn thành 100% chưa). */
  courseProgress: (courseId, studentId) =>
    progressRequest(`/courses/${courseId}?studentId=${encodeURIComponent(studentId)}`),
  /** UC-P02 — học viên tự đánh dấu bài học (Video / Bài đọc) đã hoàn thành. */
  markLessonCompleted: (courseId, lessonId, studentId) =>
    progressRequest(
      `/courses/${courseId}/lessons/${lessonId}/complete`,
      json('PUT', { studentId, courseId }),
    ),
  submitQuiz: (courseId, quizId, payload) =>
    progressRequest(`/courses/${courseId}/quizzes/${quizId}/submit`, json('POST', payload)),
  quizAttempts: (courseId, quizId, studentId) =>
    progressRequest(
      `/courses/${courseId}/quizzes/${quizId}/attempts?studentId=${encodeURIComponent(studentId)}`,
    ),
}
