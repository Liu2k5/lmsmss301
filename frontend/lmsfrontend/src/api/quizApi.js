const BASE_URL = '/api/v1/quizzes'

export class ApiError extends Error {
  constructor(message, status, body) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.body = body
    this.details = body?.details ?? []
  }
}

async function request(url, options = {}) {
  let response
  try {
    response = await fetch(url, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    })
  } catch {
    throw new ApiError('Không kết nối được tới course-service. Kiểm tra backend đã chạy ở cổng 8081 chưa.', 0)
  }

  if (response.status === 204) {
    return null
  }

  const body = await response.json().catch(() => null)
  if (!response.ok) {
    throw new ApiError(body?.message ?? `Yêu cầu thất bại (HTTP ${response.status})`, response.status, body)
  }
  return body
}

export const quizApi = {
  list: (lessonId) => request(lessonId ? `${BASE_URL}?lessonId=${lessonId}` : BASE_URL),
  get: (id) => request(`${BASE_URL}/${id}`),
  lessonOptions: () => request(`${BASE_URL}/lesson-options`),
  create: (payload) => request(BASE_URL, { method: 'POST', body: JSON.stringify(payload) }),
  update: (id, payload) => request(`${BASE_URL}/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  remove: (id) => request(`${BASE_URL}/${id}`, { method: 'DELETE' }),
}
