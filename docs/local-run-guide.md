# Hướng dẫn chạy code trên máy (Docker cho hạ tầng + IntelliJ cho service)

Cách chạy khuyên dùng khi phát triển: **Docker chỉ chạy hạ tầng** (Postgres, Redis, RabbitMQ, MinIO, Keycloak),
còn **các service Java chạy trong IntelliJ**. Không phải build image Java (tránh lỗi tải thư viện Maven trong Docker),
debug được và sửa code chỉ cần restart service.

> Muốn chạy **toàn bộ bằng Docker** thì xem README (`docker compose up -d --build`).

## 1. Yêu cầu

| Công cụ | Phiên bản |
|---|---|
| Docker Desktop | đang chạy (*Engine running*) |
| JDK | 21 |
| IntelliJ IDEA | Ultimate (có run configuration Spring Boot) hoặc Community |
| Node.js | để chạy frontend (`npm`) |

Các cổng phải trống: `5432, 6379, 5672, 9000, 9001, 8180, 8761, 8080–8085, 5173`.

## 2. Tạo `backend/.env`

```bash
cd backend
cp .env.example .env
```

Giá trị dùng để chạy trên máy (khớp với giá trị mặc định trong `application.properties`/`application.yml`
của các service, nên chạy trong IntelliJ **không cần đặt biến môi trường**):

```
POSTGRES_USER=sa
POSTGRES_PASSWORD=sa
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin123
MINIO_BUCKET=lms-materials
REDIS_PASSWORD=redis123
RABBITMQ_USER=lms
RABBITMQ_PASSWORD=rabbit123
RABBITMQ_VHOST=lms
USER_SERVICE_CLIENT_SECRET=lms-user-service-secret-2026
JWT_ISSUER=http://localhost:8180/realms/lms
JWT_JWK_SET_URI=http://keycloak:8080/realms/lms/protocol/openid-connect/certs
```

Lưu ý:
- Tên biến phải **đúng như trên** (`KEYCLOAK_ADMIN`, `USER_SERVICE_CLIENT_SECRET`…). Các tên cũ như
  `KC_ADMIN_USER`, `USER_SERVICE_KC_SECRET`, Keycloak cổng `8181` thuộc file compose cũ đã bị xoá.
- `USER_SERVICE_CLIENT_SECRET` phải khớp secret trong `backend/infras/keycloak/realm-lms.json`.
- `MINIO_ROOT_PASSWORD` tối thiểu 8 ký tự.
- **Không commit** file `.env`.

## 3. Chạy hạ tầng bằng Docker

```bash
cd backend
docker compose up -d postgres redis rabbitmq minio minio-init keycloak
docker compose ps
```

Mong đợi: 5 container `healthy`, `minio-init` ở trạng thái `Exited (0)` (đã tạo bucket rồi tự thoát — bình thường).

- `backend/docker-compose.override.yml` (Docker tự đọc kèm) thay image MinIO `aistor` bằng `bitnamilegacy/minio`:
  image `aistor` là bản thương mại, không có license thì không tạo được bucket
  (`Access denied. No license is installed`).
- Nếu trước đó đã chạy Postgres với user/mật khẩu khác thì volume cũ vẫn giữ thông tin cũ →
  `docker compose down -v` (**xoá dữ liệu**) rồi chạy lại.

## 4. Chạy các service trong IntelliJ

1. **File → Open** → thư mục `backend` (project Maven nhiều module).
2. Thêm VM option cho các service dùng database (Course, User, Enrollment, Progress, ReviewRating):
   **Run → Edit Configurations → Modify options → Add VM options**:
   ```
   -Duser.timezone=Asia/Ho_Chi_Minh
   ```
   Lý do: Windows múi giờ "SE Asia Standard Time" làm Java gửi `Asia/Saigon`, Postgres 16 không nhận →
   lỗi `FATAL: invalid value for parameter "TimeZone": "Asia/Saigon"`.
3. Bảng **Services** → `+` → **Run Configuration Type → Spring Boot** để thấy các service.
4. Chạy theo thứ tự:
   1. `EurekaServerApplication` (8761)
   2. `CourseServiceApplication` (8081), `UserServiceApplication` (8082), `EnrollmentServiceApplication` (8083),
      `ProgressServiceApplication` (8084), `ReviewRatingServiceApplication` (8085)
   3. `ApiGatewayApplication` (8080)
5. Kiểm tra http://localhost:8761 — đủ 6 service.

## 5. Chạy frontend

```bash
cd frontend/lmsfrontend
npm install
npm run dev
```

Mở http://localhost:5173. Không cần file `.env` cho frontend (code đã có giá trị mặc định đúng).
Khi dev, Vite proxy `/api/...` sang course-service (8081); enrollment / review đi qua Gateway (8080).

Tài khoản demo: `student1/student1`, `instructor1/instructor1`, `admin1/admin1`.

## 6. Kiểm thử

- Test tự động: `cd backend/services/<service>` → `./mvnw test`.
- Kiểm thử thủ công toàn hệ thống: [manual-test-plan.md](manual-test-plan.md).

## 7. Lỗi hay gặp

| Hiện tượng | Cách xử lý |
|---|---|
| `Port ... already in use` | Tắt app đang chiếm cổng (service chạy trong Docker, IntelliJ khác, Postgres cài sẵn…) |
| `Connection refused` tới 5432 / 6379 / 5672 / 9000 | Hạ tầng Docker chưa chạy — làm lại bước 3 |
| `password authentication failed for user "sa"` | Volume Postgres cũ → `docker compose down -v` |
| `invalid value for parameter "TimeZone": "Asia/Saigon"` | Thêm VM option ở bước 4.2 |
| `minio-init` báo `No license is installed` | Thiếu `backend/docker-compose.override.yml` |
| Build Docker lỗi `Premature end of Content-Length` | Mạng đứt khi Maven tải thư viện → build từng service: `docker compose build <service>`, hoặc chạy service bằng IntelliJ |
| Quên mật khẩu không gửi email | Compose chưa truyền `MAIL_*` vào user-service; khi chạy IntelliJ thì đặt `MAIL_USERNAME`, `MAIL_PASSWORD` (App password Gmail) |
