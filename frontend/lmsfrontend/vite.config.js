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
      // Chuyển tiếp mọi request /api còn lại sang course-service để tránh CORS khi dev.
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
})
