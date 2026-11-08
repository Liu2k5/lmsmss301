import { ApiError, json, request } from './http.js'

const V1 = '/api/v1'

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
  const promise = new Promise((resolve, reject) => {
    xhr.open('POST', `${V1}/lessons/${lessonId}/materials/upload`)
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
  })
  return { promise, abort: () => xhr.abort() }
}

/** Mọi thao tác ghi trên curriculum đều trả về chi tiết khoá học sau khi thay đổi. */
export const courseApi = {
  list: (filters = {}) => request(`${V1}/courses${query(filters)}`),
  get: (id) => request(`${V1}/courses/${id}`),
  create: (payload) => request(`${V1}/courses`, json('POST', payload)),
  update: (id, payload) => request(`${V1}/courses/${id}`, json('PUT', payload)),
  uploadThumbnail: (id, file) => {
    const form = new FormData()
    form.append('file', file)
    return request(`${V1}/courses/${id}/thumbnail`, { method: 'POST', body: form })
  },
  removeThumbnail: (id) => request(`${V1}/courses/${id}/thumbnail`, { method: 'DELETE' }),
  changeStatus: (id, status) => request(`${V1}/courses/${id}/status`, json('PATCH', { status })),
  remove: (id) => request(`${V1}/courses/${id}`, { method: 'DELETE' }),

  addSection: (courseId, payload) => request(`${V1}/courses/${courseId}/sections`, json('POST', payload)),
  updateSection: (id, payload) => request(`${V1}/sections/${id}`, json('PUT', payload)),
  removeSection: (id) => request(`${V1}/sections/${id}`, { method: 'DELETE' }),
  reorderSections: (courseId, orderedIds) =>
    request(`${V1}/courses/${courseId}/sections/order`, json('PUT', { orderedIds })),

  addLesson: (sectionId, payload) => request(`${V1}/sections/${sectionId}/lessons`, json('POST', payload)),
  updateLesson: (id, payload) => request(`${V1}/lessons/${id}`, json('PUT', payload)),
  removeLesson: (id) => request(`${V1}/lessons/${id}`, { method: 'DELETE' }),
  reorderLessons: (sectionId, orderedIds) =>
    request(`${V1}/sections/${sectionId}/lessons/order`, json('PUT', { orderedIds })),

  addMaterial: (lessonId, payload) => request(`${V1}/lessons/${lessonId}/materials`, json('POST', payload)),
  uploadMaterial,
  updateMaterial: (id, payload) => request(`${V1}/materials/${id}`, json('PUT', payload)),
  removeMaterial: (id) => request(`${V1}/materials/${id}`, { method: 'DELETE' }),
}
