import { ApiError, json, request } from './http.js'

const V1 = '/api/v1'

/**
 * Thao tác ghi nội dung khoá học và "Khoá học của tôi" cần JWT (backend chỉ cho INSTRUCTOR).
 * AuthProvider đăng ký hàm lấy token; request đọc công khai (danh sách/chi tiết) không gửi token.
 */
let tokenProvider = async () => null

export function setCourseTokenProvider(provider) {
  tokenProvider = typeof provider === 'function' ? provider : async () => null
}

async function courseRequest(path, options = {}) {
  const token = await tokenProvider().catch(() => null)
  return request(`${V1}${path}`, { ...options, token })
}

async function authHeader() {
  const token = await tokenProvider().catch(() => null)
  return token ? { Authorization: `Bearer ${token}` } : {}
}

function query(params) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, value)
    }
  })
  const text = search.toString()
  return text ? `?${text}` : ''
}

/**
 * Upload file lên MinIO qua course-service. Dùng XMLHttpRequest để báo tiến độ (fetch không hỗ trợ).
 * Trả về { promise, abort }; promise resolve chi tiết khoá học sau khi thêm học liệu.
 */
function uploadMaterial(lessonId, file, { name, type } = {}, onProgress) {
  const form = new FormData()
  form.append('file', file)
  if (name) form.append('name', name)
  if (type) form.append('type', type)

  const xhr = new XMLHttpRequest()
  // Giữ hàm đồng bộ (trả { promise, abort } ngay) nhưng chỉ mở request sau khi có token.
  const promise = authHeader().then((headers) => new Promise((resolve, reject) => {
    xhr.open('POST', `${V1}/lessons/${lessonId}/materials/upload`)
    Object.entries(headers).forEach(([header, value]) => xhr.setRequestHeader(header, value))
    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable && onProgress) {
        onProgress(Math.round((event.loaded / event.total) * 100))
      }
    }
    xhr.onload = () => {
      let body
      try {
        body = JSON.parse(xhr.responseText)
      } catch {
        body = null
      }
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve(body)
      } else {
        reject(new ApiError(body?.message ?? `Upload thất bại (HTTP ${xhr.status})`, xhr.status, body))
      }
    }
    xhr.onerror = () =>
      reject(new ApiError('Không kết nối được tới course-service. Kiểm tra backend đã chạy ở cổng 8081 chưa.', 0))
    xhr.onabort = () => reject(new ApiError('Đã huỷ upload.', 0))
    xhr.send(form)
  }))
  return { promise, abort: () => xhr.abort() }
}

/** Mọi thao tác ghi trên curriculum đều trả về chi tiết khoá học sau khi thay đổi. */
export const courseApi = {
  // Công khai (khách, học viên): chỉ khoá không phải bản nháp.
  list: (filters = {}) => request(`${V1}/courses${query(filters)}`),
  get: (id) => request(`${V1}/courses/${id}`),
  // Giảng viên: "Khoá học của tôi", gồm cả bản nháp (cần JWT, chỉ INSTRUCTOR).
  listMine: (filters = {}) => courseRequest(`/instructor/courses${query(filters)}`),
  getMine: (id) => courseRequest(`/instructor/courses/${id}`),
  create: (payload) => courseRequest(`/courses`, json('POST', payload)),
  update: (id, payload) => courseRequest(`/courses/${id}`, json('PUT', payload)),
  uploadThumbnail: (id, file) => {
    const form = new FormData()
    form.append('file', file)
    return courseRequest(`/courses/${id}/thumbnail`, { method: 'POST', body: form })
  },
  removeThumbnail: (id) => courseRequest(`/courses/${id}/thumbnail`, { method: 'DELETE' }),
  changeStatus: (id, status) => courseRequest(`/courses/${id}/status`, json('PATCH', { status })),
  remove: (id) => courseRequest(`/courses/${id}`, { method: 'DELETE' }),

  addSection: (courseId, payload) => courseRequest(`/courses/${courseId}/sections`, json('POST', payload)),
  updateSection: (id, payload) => courseRequest(`/sections/${id}`, json('PUT', payload)),
  removeSection: (id) => courseRequest(`/sections/${id}`, { method: 'DELETE' }),
  reorderSections: (courseId, orderedIds) =>
    courseRequest(`/courses/${courseId}/sections/order`, json('PUT', { orderedIds })),

  addLesson: (sectionId, payload) => courseRequest(`/sections/${sectionId}/lessons`, json('POST', payload)),
  updateLesson: (id, payload) => courseRequest(`/lessons/${id}`, json('PUT', payload)),
  removeLesson: (id) => courseRequest(`/lessons/${id}`, { method: 'DELETE' }),
  reorderLessons: (sectionId, orderedIds) =>
    courseRequest(`/sections/${sectionId}/lessons/order`, json('PUT', { orderedIds })),

  addMaterial: (lessonId, payload) => courseRequest(`/lessons/${lessonId}/materials`, json('POST', payload)),
  uploadMaterial,
  updateMaterial: (id, payload) => courseRequest(`/materials/${id}`, json('PUT', payload)),
  removeMaterial: (id) => courseRequest(`/materials/${id}`, { method: 'DELETE' }),
}

/**
 * Danh mục khoá học (course-service /api/v1/categories).
 * Xem: công khai. Tạo / sửa / xoá: chỉ ADMIN (backend trả 403 nếu không phải admin).
 * Mỗi danh mục có dạng { id, name, description, courseCount, createdAt, updatedAt }.
 */
export const categoryApi = {
  list: () => request(`${V1}/categories`),
  get: (id) => request(`${V1}/categories/${id}`),
  create: (payload) => courseRequest(`/categories`, json('POST', payload)),
  update: (id, payload) => courseRequest(`/categories/${id}`, json('PUT', payload)),
  remove: (id) => courseRequest(`/categories/${id}`, { method: 'DELETE' }),
}
