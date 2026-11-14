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

/** UC-R01…R11 — gửi/sửa/xoá đánh giá khoá học, xem danh sách, tương tác và quản trị. */
export const reviewApi = {
  /** Quyền viết đánh giá: đã đăng ký + đã hoàn thành khoá học, hoặc đã có đánh giá trước đó (UC-R11). */
  eligibility: (courseId) => reviewRequest(`/courses/${courseId}/reviews/eligibility`),
  /** UC-R01 — tạo đánh giá mới (1…5 sao + nhận xét). */
  create: (courseId, payload) => reviewRequest(`/courses/${courseId}/reviews`, json('POST', payload)),
  /** UC-R02 — sửa đánh giá của mình. */
  update: (reviewId, payload) => reviewRequest(`/reviews/${reviewId}`, json('PUT', payload)),
  /** UC-R03 — xoá đánh giá (người viết hoặc Admin). */
  remove: (reviewId) => reviewRequest(`/reviews/${reviewId}`, { method: 'DELETE' }),
  /** UC-R04 — danh sách đánh giá của khoá học (hỗ trợ rating, sort, page, size). */
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
  /** UC-R05 — xem tổng hợp điểm & phân bố sao của khoá học. */
  summary: (courseId) => reviewRequest(`/courses/${courseId}/ratings/summary`),
  /** UC-R06 — giảng viên hoặc Admin phản hồi nhận xét. */
  reply: (reviewId, payload) => reviewRequest(`/reviews/${reviewId}/replies`, json('POST', payload)),
  /** UC-R07 — bấm vote hữu ích (bấm lại để huỷ). */
  toggleHelpful: (reviewId) => reviewRequest(`/reviews/${reviewId}/helpful`, { method: 'POST' }),
  /** UC-R08 — báo cáo nhận xét vi phạm. */
  report: (reviewId, payload) => reviewRequest(`/reviews/${reviewId}/reports`, json('POST', payload)),
  /** UC-R09 — Admin duyệt/ẩn nhận xét (PUBLISHED / HIDDEN). */
  adminUpdateStatus: (reviewId, status) =>
    reviewRequest(`/admin/reviews/${reviewId}/status`, json('PATCH', { status })),
  /** Admin — xem danh sách báo cáo vi phạm. */
  adminReports: (params = {}) => {
    const search = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && String(value).trim() !== '') {
        search.set(key, String(value).trim())
      }
    })
    const qs = search.toString()
    return reviewRequest(`/admin/reports${qs ? `?${qs}` : ''}`)
  },
  /** Admin — cập nhật trạng thái báo cáo (RESOLVED / DISMISSED). */
  adminUpdateReportStatus: (reportId, status) =>
    reviewRequest(`/admin/reports/${reportId}/status?status=${encodeURIComponent(status)}`, { method: 'PATCH' }),
  /** UC-R10 — lịch sử đánh giá của tôi. */
  myReviews: () => reviewRequest('/my-reviews'),
}
