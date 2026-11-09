import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  envDir: '../..',
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // Enrollment (UC-E01: đăng ký khoá học) phải qua Gateway để có header X-User-Id.
      '/api/v1/enrollments': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // Progress-service (UC-P04: nộp bài quiz) — khai báo trước '/api' để thắng khi match.
      '/api/v1/progress': {
        target: 'http://localhost:8084',
        changeOrigin: true,
      },
      // Review & Rating (UC-R01…R11): đi qua Gateway để có JWT + header X-User-Id.
      // Dùng regex để thắng '/api' ở dưới cho các path lồng dưới /courses/{id}/.
      '^/api/v1/courses/[^/]+/(reviews|ratings)': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/api/v1/reviews': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/api/v1/my-reviews': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // Chuyển tiếp mọi request /api còn lại sang course-service để tránh CORS khi dev.
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
})
