# Quản lý khoá học — Course Service

Chức năng quản lý khoá học (phần Catalog: UC-C01…C13, C20…C29) gồm khoá học, chương, bài học, học liệu,
sắp xếp thứ tự, quy trình xuất bản và curriculum cho Progress Service.

- Backend: `backend/services/course-service` (port **8081**)
- Frontend: `frontend/lmsfrontend` → tab **Khoá học** (`#/courses`, `#/courses/{id}`); tab **Quiz** giữ nguyên
- Cách chạy: xem [quiz-crud-api.md](./quiz-crud-api.md#1-chạy-thử) — giống hệt
- **Khoá học / chương / bài học / học liệu / quiz / câu hỏi / đáp án đều dùng khoá chính `UUID`**; `{id}`
  trong các bảng API bên dưới là UUID (ví dụ `3f1c9b6e-5d42-4a8f-9c11-8f0f5f2d7a01`).

---

## 1. Database

Dùng lại các bảng đã có (`courses`, `sections`, `lessons`, `materials`, `quizzes`…), **không thêm cột mới**.
Hibernate `ddl-auto=update` tự tạo bảng trên PostgreSQL `course_db`.

```
courses 1──n sections 1──n lessons 1──n materials
                               └──1 quizzes 1──n questions 1──n options   (phần Quiz)
```

- `position` của chương / bài học / học liệu luôn được đánh lại liên tục 1..n sau khi thêm, xoá, sắp xếp.
- Xoá bài học ⇒ xoá kèm học liệu và quiz của bài. Xoá chương ⇒ xoá mọi bài học bên trong. Xoá khoá học ⇒ xoá tất cả.

Dữ liệu mẫu (`SEED_DEMO_DATA=true`): khoá **ReactJS** (DRAFT, 2 chương, 3 bài, 3 học liệu) và khoá **Docker**
(PUBLISHED). Mỗi khoá chỉ được tạo khi chưa có khoá cùng tên, nên khởi động lại không bị trùng; nếu xoá khoá mẫu
thì lần khởi động sau sẽ tạo lại (đặt `SEED_DEMO_DATA=false` để tắt hẳn).

## 2. Vòng đời trạng thái

Giảng viên **tự xuất bản**, không có bước admin duyệt (đề bài: "instructors can create courses").

| Từ | Có thể chuyển sang |
|---|---|
| `DRAFT` | `PUBLISHED` (xuất bản), `ARCHIVED` |
| `PUBLISHED` | `UNPUBLISHED` (ẩn), `ARCHIVED` |
| `UNPUBLISHED` | `PUBLISHED` (xuất bản lại), `DRAFT`, `ARCHIVED` |
| `ARCHIVED` | `DRAFT` (khôi phục) |

Quy tắc:

- Khoá học mới luôn là `DRAFT`.
- Chuyển sang `PUBLISHED` yêu cầu: ≥ 1 chương, mọi chương có ≥ 1 bài học, mọi bài loại `QUIZ` đã được tạo
  quiz. Sai ⇒ `400`.
- `ARCHIVED` là chỉ đọc: mọi thao tác sửa khoá học / curriculum ⇒ `400`.
- Chỉ xoá được khoá học `DRAFT`, `ARCHIVED`; trạng thái khác ⇒ `409`.
- Chuyển trạng thái không hợp lệ ⇒ `409`.
- Đổi loại bài học `QUIZ` đang có quiz sang loại khác ⇒ `409`.

Response chi tiết có sẵn `allowedTransitions` và `editable` để client tự hiển thị nút.

## 3. API

Lỗi dùng chung format `ApiError` như phần Quiz (`message`, `details[]` theo field). Gọi sai method trả `405`,
sai `Content-Type` trả `415` (không còn bị gộp thành `500`).

### Khoá học

| Method | URL | Body | Trả về |
|---|---|---|---|
| GET | `/api/v1/courses?keyword=&status=&instructorId=` | — | `CourseSummaryResponse[]` (có `sectionCount`, `lessonCount`) |
| GET | `/api/v1/courses/{id}` | — | `CourseDetailResponse` |
| POST | `/api/v1/courses` | `CourseRequest` | `201` + `CourseDetailResponse` |
| PUT | `/api/v1/courses/{id}` | `CourseRequest` | `CourseDetailResponse` |
| PATCH | `/api/v1/courses/{id}/status` | `{ "status": "PUBLISHED" }` | `CourseDetailResponse` |
| DELETE | `/api/v1/courses/{id}` | — | `204` |
| GET | `/api/v1/courses/{id}/curriculum/published` | — | `PublishedCurriculumResponse` (chỉ khi `PUBLISHED`, ngược lại `404`) — **cho Progress Service**. Chỉ gồm nội dung học được: bỏ bài Quiz chưa có quiz và chương rỗng; `totalLessons` đếm theo đó |

```json
// CourseRequest
{ "instructorId": "22222222-2222-2222-2222-222222222222", "title": "ReactJS", "description": "…", "thumbnailUrl": "https://…" }
```

### Chương, bài học, học liệu

Mọi endpoint dưới đây trả về **`CourseDetailResponse` của khoá học sau khi thay đổi** (POST trả `201`), để
client vẽ lại cả cây curriculum bằng một response.

| Method | URL | Body |
|---|---|---|
| POST | `/api/v1/courses/{courseId}/sections` | `{ "title", "description" }` |
| PUT | `/api/v1/courses/{courseId}/sections/order` | `{ "orderedIds": [3, 1, 2] }` |
| PUT | `/api/v1/sections/{id}` | `{ "title", "description" }` |
| DELETE | `/api/v1/sections/{id}` | — |
| POST | `/api/v1/sections/{sectionId}/lessons` | `{ "title", "description", "type": "VIDEO\|ARTICLE\|QUIZ", "preview": false }` |
| PUT | `/api/v1/sections/{sectionId}/lessons/order` | `{ "orderedIds": [...] }` |
| PUT | `/api/v1/lessons/{id}` | như POST lesson |
| DELETE | `/api/v1/lessons/{id}` | — |
| POST | `/api/v1/lessons/{lessonId}/materials` | Học liệu dạng link: `{ "name", "type": "VIDEO\|DOCUMENT\|IMAGE\|OTHER", "url": "https://…", "fileSize", "mimeType" }` |
| POST | `/api/v1/lessons/{lessonId}/materials/upload` | **multipart**: `file` (bắt buộc), `name`, `type` (tuỳ chọn) → upload lên MinIO |
| PUT | `/api/v1/materials/{id}` | như POST material; với file đã upload, để trống `url` = giữ nguyên file |
| DELETE | `/api/v1/materials/{id}` | — (file trên MinIO bị xoá theo) |

`orderedIds` phải chứa **đúng và đủ** id hiện có của khoá học / chương, không trùng ⇒ sai thì `400`.

### Ảnh bìa

| Method | URL | Body |
|---|---|---|
| POST | `/api/v1/courses/{id}/thumbnail` | **multipart**: `file` (image/*, ≤ 5 MB) → upload lên MinIO, thay ảnh cũ |
| DELETE | `/api/v1/courses/{id}/thumbnail` | — bỏ ảnh bìa, xoá file trên MinIO |

`PUT /courses/{id}` với `thumbnailUrl` để trống = giữ nguyên ảnh bìa hiện tại.

## 4. Lưu file trên MinIO

- Chạy MinIO local: `docker compose up -d` trong `backend/services/course-service` (API `:9000`, console `:9001`,
  tài khoản dev `minioadmin` / `minioadmin`). Bucket `course-materials` tự tạo ở lần upload đầu.
- DB lưu khoá object dạng `minio:<key>` (cột `materials.url`, `courses.thumbnail_url`). Response trả **presigned
  URL** hết hạn sau `MINIO_URL_EXPIRY_MINUTES` (mặc định 120 phút); `MaterialResponse.uploaded = true` với file trên MinIO.
- Upload chạy **ngoài transaction DB** (kiểm tra → đẩy file lên MinIO → lưu DB trong transaction ngắn), nên video
  lớn không giữ kết nối DB. Lưu DB thất bại (kể cả khi commit) thì file vừa upload bị xoá.
- File trên MinIO bị xoá **sau khi transaction commit** khi xoá / thay học liệu, bài học, chương, khoá học, ảnh bìa.
- MinIO không chạy ⇒ upload trả `503`, các API đọc vẫn chạy (link xem file = `null`).
- Biến môi trường: `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET`, `MAX_UPLOAD_SIZE` (mặc định 1GB).

## 5. Eureka (service discovery)

course-service là **Eureka client**, đăng ký với tên `COURSE-SERVICE` để Gateway gọi qua `lb://course-service`.
Eureka Server chưa có trong repo (thuộc phần hạ tầng / Gateway).

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `EUREKA_URL` | `http://localhost:8761/eureka/` | Địa chỉ Eureka Server |
| `EUREKA_ENABLED` | `true` | Đặt `false` khi chưa có Eureka Server để tắt log lỗi kết nối mỗi 30 giây |

Khi chưa có server, service **vẫn chạy bình thường**, chỉ log cảnh báo "registration failed". Spring Cloud dùng bản
`2025.1.4` (bản GA mới nhất, build cho Boot 4.0.x) và đã chạy thử được trên Boot 4.1.1; khi Spring Cloud `2026.0.x`
GA thì đổi `spring-cloud.version` trong `pom.xml`.

## 6. Việc còn lại / giới hạn

- Chưa có auth: `instructorId` (UUID = `sub` trong JWT) đang nhập tay, chưa kiểm tra giảng viên có sở hữu khoá học không. Khi Gateway
  bơm `X-User-Id` / `X-User-Role`, cần lấy `instructorId` từ header và chỉ cho giảng viên sửa khoá của mình.
- Chưa có category.
- Chưa phát event `CurriculumUpdated` cho Progress (UC-P10).

## 7. Test

- `CourseControllerIntegrationTest` (11 test, H2): tạo/sửa/tìm kiếm, validate, chương–bài học–học liệu, sắp xếp,
  xoá dây chuyền, chặn đổi loại bài có quiz, vòng đời trạng thái, curriculum published (bỏ nội dung chưa sẵn sàng),
  mã lỗi 405/415, seeder không tạo trùng.
- `MaterialUploadIntegrationTest` (14 test, MinIO được mock): upload học liệu / ảnh bìa, file rỗng / sai loại,
  khoá đã lưu trữ không upload, bài học bị xoá giữa chừng thì dọn file,
  MinIO lỗi ⇒ 503, giữ / thay / xoá file trên MinIO.
- Test tắt Eureka (`eureka.client.enabled=false`) nên không cần Eureka Server.

```bash
cd backend/services/course-service
./mvnw test
```
