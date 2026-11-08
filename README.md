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
| GET | `/api/v1/courses/{courseId}/reviews` | review |
| GET | `/api/v1/courses/{courseId}/ratings/summary` | review |

Chi tiết quiz: [docs/quiz-crud-api.md](docs/quiz-crud-api.md) · Khoá học: [docs/course-management-api.md](docs/course-management-api.md)

## Kiểm thử

```bash
cd backend
mvn -f services/course-service/pom.xml test        # 57 test
mvn -f services/enrollment-service/pom.xml test    # 59 test
mvn -f services/review-rating-service/pom.xml test # 18 test
mvn -f services/user-service/pom.xml test          # 4 test
```

## Biết trước khi tích hợp

Các nhánh được phát triển độc lập nên còn **lệch kiểu ID giữa service**:

- `course-service`, `progress-service`: `courseId`, `lessonId`, `studentId` là **Integer**.
- `enrollment-service`, `review-rating-service`, `user-service`: dùng **UUID** (khớp `sub` trong JWT).

Vì vậy luồng "đăng ký khoá học → ghi tiến độ → đánh giá" qua Gateway hiện chưa nối được end-to-end;
cần thống nhất kiểu ID (khuyến nghị UUID ở mọi service) trước khi demo trọn luồng học viên.
