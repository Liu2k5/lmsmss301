# Quiz — Course Service

Chức năng soạn quiz cho giảng viên (UC-C14 → UC-C19): quản lý quiz, câu hỏi và đáp án.

- Backend: `backend/services/course-service` (port **8081**)
- Frontend: `frontend/lmsfrontend` (Vite 5173, proxy `/api` → `localhost:8081`)
- **Mọi id (quiz, câu hỏi, đáp án, bài học, khoá học) là `UUID`**, không còn là số nguyên.

---

## 1. Chạy thử

```bash
# Backend
cd backend/services/course-service
./mvnw spring-boot:run

# Frontend (terminal khác)
cd frontend/lmsfrontend
npm install
npm run dev        # http://localhost:5173/#/quizzes
```

Biến môi trường của course-service:

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/course_db` | JDBC URL |
| `DB_USER` / `DB_PASSWORD` | `postgres` / `postgres` | Tài khoản DB |
| `DDL_AUTO` | `update` | Chiến lược schema Hibernate |
| `SERVER_PORT` | `8081` | Cổng service |
| `SEED_DEMO_DATA` | `true` | Seed dữ liệu mẫu (gồm 1 quiz demo + 1 bài QUIZ chưa có quiz) |
| `EUREKA_ENABLED` | `true` | Đăng ký Eureka (đặt `false` khi chạy một mình) |

Nếu chưa có PostgreSQL, có thể chạy tạm bằng H2 trên classpath test:

```bash
SPRING_APPLICATION_JSON='{"spring":{"datasource":{"url":"jdbc:h2:mem:devdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL","driver-class-name":"org.h2.Driver","username":"sa","password":""},"jpa":{"hibernate":{"ddl-auto":"create-drop"}}},"eureka":{"client":{"enabled":false}}}' \
  ./mvnw spring-boot:run -Dspring-boot.run.useTestClasspath=true
```

---

## 2. Mô hình dữ liệu

```
Course 1─n Section 1─n Lesson 1─1 Quiz 1─n Question 1─n Option
```

- `Lesson.type` phải là `QUIZ` mới gắn được quiz.
- Mỗi `Lesson` có **tối đa một** `Quiz` (ràng buộc unique `quizzes.lesson_id`).
- `position` do server sinh theo thứ tự gửi lên / thứ tự sắp xếp, client không gửi.

---

## 3. API

Base path: `/api/v1/quizzes`

### 3.1. Quiz

| Method | Endpoint | Mô tả | Mã |
|---|---|---|---|
| GET | `/quizzes?lessonId=&courseId=&keyword=` | Danh sách quiz (các filter kết hợp AND) | 200 |
| GET | `/quizzes/{id}` | Chi tiết đầy đủ (kèm đáp án đúng) | 200 |
| GET | `/quizzes/{id}/preview` | Bản xem trước như học viên — **không** có `correct` | 200 |
| GET | `/quizzes/lesson-options` | Bài học loại QUIZ + quiz hiện có | 200 |
| POST | `/quizzes` | Tạo quiz (kèm cây câu hỏi) | 201 |
| PUT | `/quizzes/{id}` | Cập nhật toàn bộ quiz | 200 |
| DELETE | `/quizzes/{id}` | Xoá quiz + câu hỏi + đáp án | 204 |
| POST | `/quizzes/{id}/duplicate` | Nhân bản sang bài học khác | 201 |

`keyword` tìm trong tên quiz, tên bài học và tên khoá học (không phân biệt hoa thường).

`POST /quizzes/{id}/duplicate` body:

```json
{ "lessonId": "3f1c9b6e-5d42-4a8f-9c11-8f0f5f2d7a01" }
```

### 3.2. Câu hỏi

| Method | Endpoint | Mô tả | Mã |
|---|---|---|---|
| POST | `/quizzes/{quizId}/questions` | Thêm câu hỏi vào cuối quiz | 201 |
| PUT | `/quizzes/{quizId}/questions/{questionId}` | Sửa một câu hỏi | 200 |
| DELETE | `/quizzes/{quizId}/questions/{questionId}` | Xoá câu hỏi, dồn lại `position` | 204 |
| PUT | `/quizzes/{quizId}/questions/order` | Sắp xếp lại toàn bộ câu hỏi | 200 |

Body sắp xếp — phải chứa **đúng và đủ** id câu hỏi hiện có:

```json
{ "orderedIds": [12, 9, 11] }
```

### 3.3. Request body

`position` không gửi lên (server tự sinh theo thứ tự trong mảng).

```json
{
  "lessonId": "3f1c9b6e-5d42-4a8f-9c11-8f0f5f2d7a01",
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

Khi cập nhật, gửi kèm `id` câu hỏi/đáp án đã tồn tại để giữ nguyên bản ghi; phần tử không có `id` được tạo mới;
phần tử không còn trong mảng bị xoá (orphan removal).

### 3.4. Enum

| Enum | Giá trị |
|---|---|
| `QuestionType` | `SINGLE_CHOICE`, `MULTIPLE_CHOICE`, `TRUE_FALSE` |
| `LessonType` | `VIDEO`, `ARTICLE`, `QUIZ` |

### 3.5. Quy tắc nghiệp vụ

| Quy tắc | HTTP |
|---|---|
| Quiz / câu hỏi / bài học không tồn tại | 404 |
| Bài học không phải loại `QUIZ` | 400 |
| Bài học đã có quiz (khi tạo/nhân bản) | 409 |
| Quiz phải có ≥ 1 câu hỏi; không xoá câu hỏi cuối cùng | 400 |
| Mỗi câu phải có **2–10** đáp án | 400 |
| `TRUE_FALSE` phải có đúng 2 đáp án | 400 |
| Mỗi câu phải có ≥ 1 đáp án đúng | 400 |
| `SINGLE_CHOICE` / `TRUE_FALSE` phải có **đúng 1** đáp án đúng | 400 |
| Hai đáp án trong cùng câu không được trùng nội dung | 400 |
| `passingScore` trong 0–100, tối đa 2 chữ số thập phân | 400 |
| `orderedIds` không khớp danh sách câu hỏi hiện có | 400 |

### 3.6. Định dạng lỗi

```json
{
  "timestamp": "2026-11-06T15:26:55Z",
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

## 4. Giao diện

Route: `#/quizzes`. Deep-link từ trang khoá học:

| URL | Hành vi |
|---|---|
| `#/quizzes?quizId=5` | Mở form **sửa** quiz 5 |
| `#/quizzes?lessonId=8` | Mở form **tạo** quiz cho bài học 8 |

Trong `CourseDetail`, tag “Đã có quiz” / “Chưa có quiz” là link mở đúng quiz/bài học tương ứng.

| File | Nhiệm vụ |
|---|---|
| `pages/QuizPage.jsx` | Danh sách + thống kê, tìm kiếm/lọc theo khoá học, nhân bản, xem trước, xoá, deep-link |
| `components/QuizForm.jsx` | Form tạo/sửa: chọn bài học, câu hỏi (lên/xuống/nhân bản/xoá), đáp án, đổi loại câu hỏi |
| `components/QuizPreview.jsx` | Xem trước như học viên, nút “Hiện đáp án” cho giảng viên |
| `api/quizApi.js` | Lớp gọi API quiz |

Chi tiết UI:

- **Danh sách**: tìm kiếm (debounce 300 ms), lọc theo khoá học, đếm quiz/câu hỏi/bài học còn trống.
- **Form**: thêm/xoá/đổi loại câu hỏi, đổi thứ tự bằng ↑ ↓, nhân bản câu hỏi, đánh dấu đáp án đúng
  (radio cho một đáp án, checkbox cho nhiều đáp án), validate phía client trước khi gửi.
- **Xem trước**: hiển thị đúng như học viên, không lộ đáp án; bấm “Hiện đáp án” mới tô xanh đáp án đúng.
- **Nhân bản**: chọn bài học đích trong danh sách bài QUIZ chưa có quiz.

---

## 5. Kiểm thử

```bash
cd backend/services/course-service
./mvnw test             # 57 test
cd ../../../frontend/lmsfrontend
npm run lint && npm run build
```

| Test class | Nội dung |
|---|---|
| `QuizControllerIntegrationTest` (14) | CRUD quiz qua MockMvc, cascade, trùng bài học, validate, 404 |
| `QuizQuestionApiIntegrationTest` (15) | Thêm/sửa/xoá/sắp xếp câu hỏi, nhân bản, preview, filter, validate đáp án |
| `CourseCurriculumMappingTests` (2) | Mapping entity |
| `CourseControllerIntegrationTest` (11), `MaterialUploadIntegrationTest` (14) | Khoá học, học liệu |
