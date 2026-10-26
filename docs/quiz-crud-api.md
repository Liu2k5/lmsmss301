# Quiz CRUD — Course Service

Tài liệu cho chức năng quản lý bài quiz (UC-C14 → UC-C19) và giao diện đi kèm.

- Backend: `backend/services/course-service` (port **8081**)
- Frontend: `frontend/lmsfrontend` (Vite, port 5173, proxy `/api` → `localhost:8081`)

---

## 1. Chạy thử

```bash
# Backend
cd backend/services/course-service
./mvnw spring-boot:run

# Frontend (terminal khác)
cd frontend/lmsfrontend
npm install
npm run dev        # http://localhost:5173
```

Backend cần PostgreSQL ở `localhost:5432/course_db`. Có thể override bằng biến môi trường:

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/course_db` | JDBC URL |
| `DB_USER` | `postgres` | Tài khoản DB |
| `DB_PASSWORD` | `postgres` | Mật khẩu DB |
| `DDL_AUTO` | `update` | Chiến lược schema của Hibernate |
| `SERVER_PORT` | `8081` | Cổng service |
| `SEED_DEMO_DATA` | `true` | Tạo dữ liệu mẫu khi DB trống (tắt bằng `false`) |

Dữ liệu mẫu gồm 1 khoá học, 3 bài học (1 VIDEO, 2 QUIZ), trong đó 1 bài học QUIZ đã có quiz và 1 bài để trống
cho việc tạo mới.

---

## 2. API

Base path: `/api/v1/quizzes`

| Method | Endpoint | Mô tả | Mã thành công |
|---|---|---|---|
| GET | `/api/v1/quizzes?lessonId=` | Danh sách quiz (lọc theo bài học nếu có) | 200 |
| GET | `/api/v1/quizzes/{id}` | Chi tiết quiz kèm câu hỏi và đáp án | 200 |
| GET | `/api/v1/quizzes/lesson-options` | Bài học loại QUIZ + quiz hiện có (cho dropdown) | 200 |
| POST | `/api/v1/quizzes` | Tạo quiz | 201 |
| PUT | `/api/v1/quizzes/{id}` | Cập nhật quiz | 200 |
| DELETE | `/api/v1/quizzes/{id}` | Xoá quiz (cascade câu hỏi + đáp án) | 204 |

### 2.1. Request body

`position` **không** gửi lên — server tự sinh theo thứ tự trong mảng.

```json
{
  "lessonId": 3,
  "title": "Kiểm tra cuối chương 1",
  "description": "Chọn đáp án đúng nhất.",
  "passingScore": 70,
  "questions": [
    {
      "content": "Spring Boot là gì?",
      "type": "SINGLE_CHOICE",
      "options": [
        { "content": "Một framework của Spring", "correct": true },
        { "content": "Một ngôn ngữ lập trình", "correct": false }
      ]
    }
  ]
}
```

Khi cập nhật, gửi kèm `id` của câu hỏi/đáp án đã tồn tại để giữ nguyên bản ghi; phần tử không có `id` sẽ được
tạo mới, phần tử không còn trong mảng sẽ bị xoá.

### 2.2. Enum

| Enum | Giá trị |
|---|---|
| `QuestionType` | `SINGLE_CHOICE`, `MULTIPLE_CHOICE`, `TRUE_FALSE` |
| `LessonType` | `VIDEO`, `ARTICLE`, `QUIZ` |

### 2.3. Quy tắc nghiệp vụ (400/409)

| Quy tắc | Mã lỗi |
|---|---|
| Bài học phải tồn tại | 404 |
| Chỉ bài học loại `QUIZ` mới gắn được quiz | 400 |
| Mỗi bài học chỉ có **một** quiz (ràng buộc unique `quizzes.lesson_id`) | 409 |
| Quiz phải có ≥ 1 câu hỏi, mỗi câu ≥ 1 đáp án, nội dung không rỗng | 400 |
| Mỗi câu phải có ≥ 1 đáp án đúng | 400 |
| `SINGLE_CHOICE` / `TRUE_FALSE` phải có **đúng** 1 đáp án đúng | 400 |
| `passingScore` trong khoảng 0–100, tối đa 2 chữ số thập phân | 400 |

### 2.4. Định dạng lỗi

```json
{
  "timestamp": "2026-10-26T15:26:55Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Dữ liệu không hợp lệ",
  "path": "/api/v1/quizzes",
  "details": [
    { "field": "title", "message": "Tên quiz không được để trống" }
  ]
}
```

---

## 3. Giao diện

`frontend/lmsfrontend/src`

| File | Nhiệm vụ |
|---|---|
| `App.jsx` | Trang quản lý: thống kê, danh sách quiz, xoá có xác nhận, toast |
| `components/QuizForm.jsx` | Modal tạo/sửa: chọn bài học, thông tin quiz, danh sách câu hỏi và đáp án |
| `api/quizApi.js` | Lớp gọi API, chuẩn hoá lỗi thành `ApiError` |
| `App.css`, `index.css` | Giao diện |

Chức năng trên UI: xem danh sách, tạo, sửa (giữ id câu hỏi/đáp án), xoá, đổi loại câu hỏi, thêm/xoá câu hỏi và
đáp án, đánh dấu đáp án đúng, validate phía client trước khi gửi và hiển thị lỗi validate từ server theo từng field.

---

## 4. Kiểm thử

```bash
cd backend/services/course-service
./mvnw test          # 17 test: mapping entity + CRUD integration (MockMvc, H2)
```

`QuizControllerIntegrationTest` phủ: tạo, danh sách, chi tiết, cập nhật (thay đổi và xoá câu hỏi, giữ id),
xoá + cascade, trùng bài học (409), sai loại bài học (400), không có đáp án đúng (400), nhiều đáp án đúng cho
câu hỏi một đáp án (400), validate field, danh sách lesson-options, 404.
