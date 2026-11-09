import { ApiError, json, request } from './http.js'

const BASE_URL = '/api/v1/quizzes'

export { ApiError }

/**
 * Quiz chỉ dành cho giảng viên/admin nên mọi request phải kèm JWT.
 * AuthProvider đăng ký hàm lấy token khi khởi tạo; nếu chưa đăng nhập, request đi không token
 * và backend trả 401 (UI hiển thị yêu cầu đăng nhập).
 */
let tokenProvider = async () => null

export function setQuizTokenProvider(provider) {
  tokenProvider = typeof provider === 'function' ? provider : async () => null
}

async function quizRequest(path, options = {}) {
  const token = await tokenProvider().catch(() => null)
  return request(`${BASE_URL}${path}`, { ...options, token })
}

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
  list: (params) => quizRequest(query(params)),
  get: (id) => quizRequest(`/${id}`),
  preview: (id) => quizRequest(`/${id}/preview`),
  lessonOptions: () => quizRequest('/lesson-options'),
  create: (payload) => quizRequest('', json('POST', payload)),
  update: (id, payload) => quizRequest(`/${id}`, json('PUT', payload)),
  remove: (id) => quizRequest(`/${id}`, { method: 'DELETE' }),
  duplicate: (id, lessonId) => quizRequest(`/${id}/duplicate`, json('POST', { lessonId })),

  // ------------------------------------------------------------ câu hỏi
  addQuestion: (quizId, payload) => quizRequest(`/${quizId}/questions`, json('POST', payload)),
  updateQuestion: (quizId, questionId, payload) =>
    quizRequest(`/${quizId}/questions/${questionId}`, json('PUT', payload)),
  removeQuestion: (quizId, questionId) =>
    quizRequest(`/${quizId}/questions/${questionId}`, { method: 'DELETE' }),
  reorderQuestions: (quizId, orderedIds) =>
    quizRequest(`/${quizId}/questions/order`, json('PUT', { orderedIds })),
}
