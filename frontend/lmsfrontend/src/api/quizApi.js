import { ApiError, json, request } from './http.js'

const BASE_URL = '/api/v1/quizzes'

export { ApiError }

function query(params) {
  const search = new URLSearchParams()
  Object.entries(params ?? {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      search.set(key, String(value).trim())
    }
  })
  const qs = search.toString()
  return qs ? `?${qs}` : ''
}

export const quizApi = {
  /** @param {{lessonId?: number|string, courseId?: number|string, keyword?: string}} [params] */
  list: (params) => request(`${BASE_URL}${query(params)}`),
  get: (id) => request(`${BASE_URL}/${id}`),
  preview: (id) => request(`${BASE_URL}/${id}/preview`),
  lessonOptions: () => request(`${BASE_URL}/lesson-options`),
  create: (payload) => request(BASE_URL, json('POST', payload)),
  update: (id, payload) => request(`${BASE_URL}/${id}`, json('PUT', payload)),
  remove: (id) => request(`${BASE_URL}/${id}`, { method: 'DELETE' }),
  duplicate: (id, lessonId) => request(`${BASE_URL}/${id}/duplicate`, json('POST', { lessonId })),

  // ------------------------------------------------------------ câu hỏi
  addQuestion: (quizId, payload) => request(`${BASE_URL}/${quizId}/questions`, json('POST', payload)),
  updateQuestion: (quizId, questionId, payload) =>
    request(`${BASE_URL}/${quizId}/questions/${questionId}`, json('PUT', payload)),
  removeQuestion: (quizId, questionId) =>
    request(`${BASE_URL}/${quizId}/questions/${questionId}`, { method: 'DELETE' }),
  reorderQuestions: (quizId, orderedIds) =>
    request(`${BASE_URL}/${quizId}/questions/order`, json('PUT', { orderedIds })),
}
