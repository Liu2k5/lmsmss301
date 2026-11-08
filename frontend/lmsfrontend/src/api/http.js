export class ApiError extends Error {
  constructor(message, status, body) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.body = body
    this.details = body?.details ?? []
  }
}

/** Gọi REST API của course-service, ném ApiError (kèm details theo field) khi lỗi. */
export async function request(url, options = {}) {
  let response
  const { token, errorMessage, ...fetchOptions } = options
  try {
    // FormData (upload file) phải để trình duyệt tự đặt Content-Type kèm boundary.
    const isForm = fetchOptions.body instanceof FormData
    const headers = new Headers(fetchOptions.headers)
    if (!isForm && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json')
    }
    if (token) {
      headers.set('Authorization', `Bearer ${token}`)
    }
    response = await fetch(url, {
      ...fetchOptions,
      headers,
    })
  } catch {
    throw new ApiError(
      errorMessage ?? 'Không kết nối được tới course-service. Kiểm tra backend đã chạy ở cổng 8081 chưa.',
      0,
    )
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

export function json(method, payload) {
  return { method, body: JSON.stringify(payload) }
}
