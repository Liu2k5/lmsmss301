# lmsmss301 — LearnHub LMS

Hệ thống học trực tuyến theo kiến trúc microservices: **API Gateway + 5 service + Keycloak + hạ tầng**.

## Thành phần

| Thành phần | Cổng | Mô tả |
|---|---|---|
| `api-gateway` | 8080 | Spring Cloud Gateway (định tuyến qua Eureka, xác thực JWT Keycloak) |
| `user-service` | 8082 | Tài khoản, hồ sơ, đăng ký, Keycloak admin |
| `course-service` | 8081 | Khoá học, chương, bài học, học liệu, quiz |
| `enrollment-service` | 8083 | Đăng ký / huỷ đăng ký khoá học |
| `progress-service` | 8084 | Tiến độ học, điểm quiz, chứng chỉ |
| `review-rating-service` | 8085 | Đánh giá, bình luận, rating |
| `eureka-server` | 8761 | Service discovery |
| PostgreSQL | 5432 | 6 database (init tự động) |
| Keycloak | 8180 | Đăng nhập, phát hành JWT |
| Redis / RabbitMQ | 6379 / 5672 | Cache / event |
| MinIO | 9000, 9001 | Lưu học liệu (console 9001) |

## Chạy toàn bộ hệ thống

```bash
cd backend
cp .env.example .env          # đổi mật khẩu nếu cần
docker compose up -d --build
```

Kiểm tra:

```bash
docker compose ps                                  # tất cả phải (healthy)
curl http://localhost:8761/eureka/apps             # 6 service đăng ký
curl http://localhost:8080/actuator/health         # gateway UP
```

> Cổng **8180** phải trống trước khi start (Keycloak). Nếu một Keycloak khác đang chạy, dừng nó trước.

### Tài khoản demo (Keycloak realm `lms`)

| Username | Mật khẩu | Role |
|---|---|---|
| `student1` | `student1` | student |
| `instructor1` | `instructor1` | instructor |
| `admin1` | `admin1` | admin |

Lấy token để gọi API trực tiếp:

```bash
TOKEN=$(curl -s -X POST http://localhost:8180/realms/lms/protocol/openid-connect/token \
  -d grant_type=password -d client_id=lms-web \
  -d username=student1 -d password=student1 | jq -r .access_token)

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/courses
```

## Chạy frontend

```bash
cd frontend/lmsfrontend
cp .env.example .env
npm install
npm run dev        # http://localhost:5173
```

Đăng nhập bằng tài khoản demo ở trên (Keycloak tại `http://localhost:8180`).

## Endpoint tiêu biểu

| Method | Endpoint | Service |
|---|---|---|
| GET | `/api/v1/courses` | course |
| GET/POST/PUT/DELETE | `/api/v1/quizzes[/{id}]` | course |
| POST | `/api/v1/quizzes/{id}/questions` | course |
| GET | `/api/v1/users/me` | user |
| POST | `/api/v1/enrollments` | enrollment |
| GET | `/api/v1/enrollments/me` | enrollment |
| GET | `/api/v1/progress/courses/{courseId}?studentId=` | progress |
| POST | `/api/v1/progress/courses/{courseId}/quizzes/{quizId}/submit` | progress |
| GET | `/api/v1/progress/courses/{courseId}/quizzes/{quizId}/attempts` | progress |
| GET | `/api/v1/courses/{courseId}/reviews` | review |
| GET | `/api/v1/courses/{courseId}/ratings/summary` | review |

Chi tiết quiz: [docs/quiz-crud-api.md](docs/quiz-crud-api.md) · Khoá học: [docs/course-management-api.md](docs/course-management-api.md)

**Dữ liệu mẫu** (`SEED_DEMO_DATA=true`): khoá *Lập trình Java Spring Boot* có 2 chương / 4 bài học, trong đó
**3 quiz mẫu** — *Kiểm tra cuối chương 1* (2 câu), *Kiểm tra cuối chương 2* (3 câu) và *Kiểm tra tổng hợp
Spring Boot* (3 câu, có câu đúng/sai và câu nhiều đáp án).

## Kiểm thử

```bash
cd backend
mvn -f services/course-service/pom.xml test        # 73 test
mvn -f services/enrollment-service/pom.xml test    # 61 test
mvn -f services/progress-service/pom.xml test      # 10 test
mvn -f services/review-rating-service/pom.xml test # 18 test
mvn -f services/user-service/pom.xml test          # 4 test
mvn test                                           # toàn bộ 7 module
```

## Biết trước khi tích hợp

- **Mọi service dùng khoá chính `UUID`** (khoá học, chương, bài học, học liệu, quiz, câu hỏi, đáp án,
  `courseId`, `lessonId`, `studentId`). `studentId`/`instructorId` khớp `sub` trong JWT.
- **Role đồng bộ với Keycloak**: realm dùng `student` / `instructor` / `admin`; user-service ánh xạ sang
  `STUDENT` / `INSTRUCTOR` / `ADMINISTRATOR` cho `@PreAuthorize`.
- **Quyền ghi nội dung khoá học**: Course Service đọc catalog công khai, nhưng mọi API ghi (`POST`/`PUT`/
  `PATCH`/`DELETE` khoá học, chương, bài học, học liệu, quiz) chỉ cho `instructor`/`admin`; giảng viên chỉ
  thao tác trên khoá học mình phụ trách (`instructorId` = `sub` trong JWT). Học viên nhận `403`, không có
  token nhận `401` — chi tiết ở [docs/course-management-api.md](docs/course-management-api.md).
- **Enroll tự tạo tiến độ**: `POST /api/v1/enrollments` phát event `student.enrolled` (RabbitMQ) sau khi
  transaction commit; Progress Service tạo `course_progress` + `lesson_progress` từ curriculum đã xuất bản.
  Khoá chưa xuất bản thì chưa tạo tiến độ (không có bài học để theo dõi).
- **Tham gia khoá học trên UI**: học viên bấm **“＋ Tham gia khoá học”** ở trang chi tiết khoá học
  (`#/courses/{id}`) khi khoá đang `PUBLISHED`; đã đăng ký thì nút đổi thành nhãn “✓ Đã tham gia khoá học”.
- **Huỷ đăng ký (UC-E02 → UC-P11)**: học viên bấm **“Huỷ đăng ký”** ở **thẻ khoá học trong danh sách**
  (`#/courses`) hoặc ở trang chi tiết khoá học; Enrollment
  Service chuyển enrollment sang `CANCELLED`, thu hồi quyền truy cập và phát `student.unenrolled` (RabbitMQ) →
  Progress Service lưu trữ tiến độ vào `archived_progress` rồi xoá khỏi bảng hoạt động. Đăng ký lại tạo tiến độ
  mới (mỗi lần kích hoạt có `eventId` riêng nên không bị bỏ qua vì trùng event).
- **Khoá học Bản nháp bị ẩn khỏi học viên**: `GET /courses` bỏ khoá `DRAFT` và `GET /courses/{id}` trả `404`
  với học viên; giao diện cũng ẩn thẻ thống kê/lựa chọn lọc “Bản nháp”. Giảng viên sở hữu và Admin vẫn thấy.
- **Nộp quiz (UC-P04)**: học viên nộp bài ở `#/quiz-attempt?quizId=&courseId=`; Progress Service chấm điểm
  bằng đề + đáp án lấy từ API nội bộ `/internal/quizzes/{id}/answer-key` của Course Service
  (Gateway chặn `/internal/**`), lưu lịch sử lượt làm và chặn khi hết `maxAttempts`.
- **Email**: chưa cấu hình SMTP nên `POST /api/v1/users/password/forgot` trả `503`
  (`Email could not be sent`) cho tới khi bổ sung SMTP/MailHog.
