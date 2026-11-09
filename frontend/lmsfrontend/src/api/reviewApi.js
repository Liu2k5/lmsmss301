import { json, request } from './http.js'

// Review & Rating đi qua API Gateway: service này tin header X-User-Id/X-User-Role do Gateway bơm.
const API_URL = import.meta.env.VITE_REVIEW_API_URL ?? 'http://localhost:8080'
const V1 = `${API_URL}/api/v1`

let tokenProvider = async () => null

export function setReviewTokenProvider(provider) {
  tokenProvider = typeof provider === 'function' ? provider : async () => null
}

async function reviewRequest(path, options = {}) {
  const token = await tokenProvider().catch(() => null)
  return request(`${V1}${path}`, { ...options, token })
}

/** UC-R01…R04 — gửi/sửa đánh giá khoá học và đọc danh sách đánh giá. */
export const reviewApi = {
  /** Quyền viết đánh giá: đã đăng ký + đã hoàn thành khoá học, hoặc đã có đánh giá trước đó. */
  eligibility: (courseId) => reviewRequest(`/courses/${courseId}/reviews/eligibility`),
  /** UC-R01 — tạo đánh giá mới (1…5 sao + nhận xét). */
  create: (courseId, payload) => reviewRequest(`/courses/${courseId}/reviews`, json('POST', payload)),
  /** UC-R02 — sửa đánh giá của mình. */
  update: (reviewId, payload) => reviewRequest(`/reviews/${reviewId}`, json('PUT', payload)),
  /** UC-R04 — danh sách đánh giá của khoá học (mới nhất trước). */
  listByCourse: (courseId, params = {}) => {
    const search = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && String(value).trim() !== '') {
        search.set(key, String(value).trim())
      }
    })
    const qs = search.toString()
    return reviewRequest(`/courses/${courseId}/reviews${qs ? `?${qs}` : ''}`)
  },
  myReviews: () => reviewRequest('/my-reviews'),
}
