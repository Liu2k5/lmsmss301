import { json, request } from './http.js'

const API_URL = import.meta.env.VITE_USER_API_URL || 'http://localhost:8083'
const V1 = `${API_URL}/api/v1`
const NETWORK_ERROR = 'Không kết nối được tới user-service. Kiểm tra backend đã chạy ở cổng 8083 chưa.'

function userRequest(path, token, options = {}) {
  return request(`${V1}${path}`, {
    ...options,
    token,
    errorMessage: NETWORK_ERROR,
  })
}

export const userApi = {
  register: (payload) => userRequest('/users/register', null, json('POST', payload)),
  universities: () => userRequest('/universities'),
  me: (token) => userRequest('/users/me', token),
  updateMe: (token, payload) => userRequest('/users/me', token, json('PUT', payload)),
  listUsers: (token, page = 0, size = 20) =>
    userRequest(`/users/admin?page=${page}&size=${size}&sort=username,asc`, token),
  getAdminUser: (token, username) =>
    userRequest(`/users/admin/${encodeURIComponent(username)}`, token),
  createAdminUser: (token, payload) =>
    userRequest('/users/admin', token, json('POST', payload)),
  updateUser: (token, username, payload) =>
    userRequest(`/users/admin/${encodeURIComponent(username)}`, token, json('PUT', payload)),
  setUserEnabled: (token, username, enabled) =>
    userRequest(`/users/admin/${encodeURIComponent(username)}/enabled`, token,
      json('PATCH', { enabled })),
}
