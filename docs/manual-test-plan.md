# Kịch bản kiểm thử thủ công — LearnHub LMS

Mục đích: tự kiểm tra toàn bộ chức năng trên giao diện (http://localhost:5173) và một số API qua Gateway
(http://localhost:8080). Mỗi ca kiểm thử có **Mã**, **Các bước**, **Kết quả mong đợi** và cột **KQ** để tự ghi
`✅ / ❌ + ghi chú`.

---

## 0. Chuẩn bị

### 0.1 Khởi động hệ thống

| # | Việc cần làm | Kiểm tra |
|---|---|---|
| 1 | `cd backend` → `docker compose up -d postgres redis rabbitmq minio minio-init keycloak` | `docker compose ps`: 5 container `healthy`, `minio-init` = `Exited (0)` |
| 2 | IntelliJ: chạy `EurekaServerApplication` | http://localhost:8761 mở được |
| 3 | Chạy 5 service: Course, User, Enrollment, Progress, ReviewRating (VM option `-Duser.timezone=Asia/Ho_Chi_Minh`) | Log có `Started ...Application` |
| 4 | Chạy `ApiGatewayApplication` | Eureka liệt kê đủ 6 service |
| 5 | `cd frontend/lmsfrontend` → `npm run dev` | http://localhost:5173 mở được |

### 0.2 Tài khoản demo (Keycloak realm `lms`)

| Username | Mật khẩu | Role | ID (`sub`) |
|---|---|---|---|
| `student1` | `student1` | student | `11111111-1111-1111-1111-111111111111` |
| `instructor1` | `instructor1` | instructor | `22222222-2222-2222-2222-222222222222` |
| `admin1` | `admin1` | admin | `33333333-3333-3333-3333-333333333333` |

### 0.3 Dữ liệu mẫu (tự tạo khi course-service khởi động)

| Khoá học | Giảng viên | Trạng thái | Nội dung |
|---|---|---|---|
| Lập trình Java Spring Boot | `instructor1` | PUBLISHED | 2 chương / 4 bài, 3 quiz mẫu |
| ReactJS từ cơ bản đến nâng cao | `instructor1` | **DRAFT** | 2 chương / 3 bài, có học liệu |
| Docker cho lập trình viên | giảng viên khác (`4444…`) | PUBLISHED | 1 chương / 1 bài |

> Muốn chạy lại từ dữ liệu sạch: dừng các service, `docker compose down -v`, rồi làm lại bước 0.1
> (**lệnh này xoá toàn bộ dữ liệu**).

---

## 1. Xác thực & tài khoản (user-service + Keycloak)

| Mã | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|
| AUTH-01 | Bấm **Đăng nhập** → nhập `student1/student1` | Quay về app, thanh menu hiện tên người dùng + mục **Hồ sơ** | |
| AUTH-02 | Đăng nhập với mật khẩu sai | Keycloak báo sai thông tin, không vào được app | |
| AUTH-03 | Đăng xuất | Menu quay lại nút **Đăng nhập / Đăng ký** | |
| AUTH-04 | **Đăng ký** tài khoản mới (username, email chưa dùng) | Đăng ký thành công, đăng nhập được bằng tài khoản vừa tạo | |
| AUTH-05 | Đăng ký lại với username hoặc email đã tồn tại | Báo lỗi trùng (`Username or email already exists`) | |
| AUTH-06 | Đăng ký để trống / sai định dạng email | Báo lỗi tại ô nhập, không gửi form | |
| AUTH-07 | **Quên mật khẩu** → nhập email | Hiện thông báo đã gửi. *Email chỉ thật sự được gửi khi đã cấu hình `MAIL_*`* | |
| AUTH-08 | **Hồ sơ** (`#/profile`) → sửa thông tin → Lưu | Lưu thành công, tải lại trang vẫn giữ giá trị mới | |
| AUTH-09 | Hồ sơ → gửi **yêu cầu lên giảng viên** | Yêu cầu ở trạng thái chờ duyệt | |
| AUTH-10 | Gửi lại yêu cầu cùng loại khi yêu cầu cũ đang chờ | Báo `Đã có yêu cầu cùng loại đang chờ xử lý` | |
| AUTH-11 | Hồ sơ → gửi **yêu cầu xoá tài khoản** | Yêu cầu ở trạng thái chờ duyệt | |

---

## 2. Danh mục khoá học (chỉ Admin sửa)

| Mã | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|
| CAT-01 | Khách mở trang **Khoá học** → ô lọc danh mục | Thấy danh sách danh mục (xem công khai) | |
| CAT-02 | `admin1` → **#/admin/categories** → Thêm danh mục mới | Danh mục xuất hiện trong danh sách | |
| CAT-03 | Thêm danh mục trùng tên | Báo lỗi trùng (`Danh mục '...' đã tồn tại`) | |
| CAT-04 | Sửa tên / mô tả danh mục | Cập nhật thành công | |
| CAT-05 | Xoá danh mục **đang có khoá học** | Bị chặn, báo đang được dùng | |
| CAT-06 | Xoá danh mục không có khoá học | Xoá thành công | |
| CAT-07 | `instructor1` / `student1` vào `#/admin/categories` | Không có menu, không thao tác được (API trả 403) | |

---

## 3. Khoá học — xem công khai (khách & học viên)

| Mã | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|
| CRS-01 | Chưa đăng nhập → **Khoá học** | Chỉ thấy khoá **không phải bản nháp** (Spring Boot, Docker). **Không** thấy ReactJS (DRAFT) | |
| CRS-02 | Tìm theo từ khoá `docker` | Chỉ còn khoá Docker | |
| CRS-03 | Lọc theo danh mục | Chỉ còn khoá thuộc danh mục đó | |
| CRS-04 | Ô lọc trạng thái | Không có lựa chọn **Nháp** | |
| CRS-05 | Mở chi tiết khoá Spring Boot | Thấy mô tả, chương, bài học; không có nút sửa | |
| CRS-06 | Gõ thẳng `#/courses/<id khoá ReactJS>` khi chưa đăng nhập / bằng `student1` | Báo **không tìm thấy** khoá học (404) | |
| CRS-07 | `student1` → **Khoá học** | Giống CRS-01, không thấy bản nháp | |

---

## 4. Khoá học — "Khoá học của tôi" (giảng viên)

| Mã | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|
| INS-01 | `instructor1` → **Khoá học** | Tiêu đề **"Khoá học của tôi"**; thấy Spring Boot **và ReactJS (Nháp)**; **không** thấy Docker (của giảng viên khác) | |
| INS-02 | Lọc trạng thái **Nháp** | Chỉ còn ReactJS | |
| INS-03 | Mở chi tiết ReactJS | Mở được, có các nút sửa / thêm chương / thêm bài | |
| INS-04 | **Tạo khoá học** (tiêu đề, mô tả, danh mục) | Khoá mới trạng thái **Nháp** hiện ngay trong danh sách | |
| INS-05 | Tạo khoá học bỏ trống tiêu đề | Báo lỗi bắt buộc nhập | |
| INS-06 | Sửa tiêu đề / mô tả / danh mục | Lưu thành công | |
| INS-07 | Upload **ảnh bìa** (JPG/PNG ≤ 5 MB) | Ảnh hiện trên thẻ khoá học và trang chi tiết | |
| INS-08 | Upload ảnh bìa là file không phải ảnh, hoặc > 5 MB | Báo `Ảnh bìa phải là file hình ảnh` / `Ảnh bìa tối đa 5 MB` | |
| INS-09 | Xoá ảnh bìa | Ảnh bìa biến mất | |

---

## 5. Nội dung khoá học (chương, bài học, học liệu)

Dùng khoá vừa tạo ở INS-04.

| Mã | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|
| CUR-01 | Thêm 2 chương | Hiện theo thứ tự 1, 2 | |
| CUR-02 | Đổi thứ tự 2 chương | Thứ tự được lưu, tải lại trang vẫn đúng | |
| CUR-03 | Sửa tên chương | Lưu thành công | |
| CUR-04 | Thêm bài học loại **VIDEO**, **ARTICLE**, **QUIZ** vào chương 1 | 3 bài hiện đúng loại | |
| CUR-05 | Đổi thứ tự bài học trong chương | Thứ tự được lưu | |
| CUR-06 | Thêm học liệu bằng **URL** (video / tài liệu) | Học liệu hiện trong bài học | |
| CUR-07 | Thêm học liệu bằng URL để trống | Báo `URL học liệu không được để trống` | |
| CUR-08 | **Upload** file học liệu (video/PDF) | Có thanh tiến độ; xong thì học liệu hiện và mở/xem được | |
| CUR-09 | Sửa / xoá học liệu | Thành công | |
| CUR-10 | Xoá bài học, xoá chương | Thành công; thứ tự còn lại được đánh số lại | |

---

## 6. Vòng đời khoá học

Trạng thái hợp lệ: **DRAFT → PUBLISHED ⇄ UNPUBLISHED**, mọi trạng thái → **ARCHIVED**, **ARCHIVED → DRAFT**.

| Mã | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|
| LIFE-01 | Xuất bản khoá **chưa có chương nào** | Báo `Khoá học phải có ít nhất 1 chương trước khi xuất bản` | |
| LIFE-02 | Xuất bản khoá có bài **QUIZ chưa tạo quiz** | Báo bài học loại QUIZ chưa được tạo quiz | |
| LIFE-03 | Hoàn thiện nội dung → **Xuất bản** | Trạng thái **PUBLISHED**; `student1` / khách giờ thấy khoá này | |
| LIFE-04 | **Ẩn** (UNPUBLISHED) | Học viên / khách không còn thấy trong danh sách công khai | |
| LIFE-05 | Xuất bản lại từ UNPUBLISHED | Hiện lại với học viên | |
| LIFE-06 | **Lưu trữ** (ARCHIVED) rồi thử sửa nội dung | Báo `Khoá học đã lưu trữ, hãy khôi phục về DRAFT trước khi chỉnh sửa` | |
| LIFE-07 | Khôi phục ARCHIVED → DRAFT | Sửa được bình thường | |
| LIFE-08 | Xoá khoá đang **PUBLISHED** | Bị chặn: phải ẩn rồi lưu trữ trước | |
| LIFE-09 | Xoá khoá **DRAFT** | Xoá thành công, biến khỏi danh sách | |

---

## 7. Quiz (giảng viên soạn)

| Mã | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|
| QZ-01 | `instructor1` → **Quiz** (`#/quizzes`) | Thấy danh sách quiz (có 3 quiz mẫu của khoá Spring Boot) | |
| QZ-02 | Tạo quiz, gắn vào bài học loại **QUIZ** | Tạo thành công | |
| QZ-03 | Gắn quiz vào bài học **không phải** loại QUIZ | Báo `Chỉ bài học loại QUIZ mới có thể gắn quiz` | |
| QZ-04 | Tạo quiz thứ 2 cho cùng bài học | Báo `Mỗi bài học chỉ có một quiz` | |
| QZ-05 | Thêm câu hỏi **một đáp án**, **nhiều đáp án**, **đúng/sai** | Lưu thành công | |
| QZ-06 | Câu hỏi chỉ có 1 lựa chọn / đáp án trùng nội dung / đúng-sai không đủ 2 đáp án | Báo lỗi tương ứng, không lưu | |
| QZ-07 | Đổi thứ tự câu hỏi | Thứ tự được lưu | |
| QZ-08 | Xoá câu hỏi cuối cùng của quiz | Báo `Quiz phải có ít nhất 1 câu hỏi` | |
| QZ-09 | **Nhân bản** quiz sang một bài học QUIZ khác | Tạo bản sao đủ câu hỏi | |
| QZ-10 | Đổi loại bài học đang có quiz sang VIDEO | Báo `Hãy xoá quiz trước khi đổi loại bài học` | |
| QZ-11 | `student1` → **Quiz** | Chỉ thấy quiz của khoá **đã đăng ký**; không có nút tạo / sửa / xoá | |

---

## 8. Đăng ký học (enrollment-service)

| Mã | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|
| ENR-01 | Khách mở chi tiết khoá PUBLISHED | Có lời nhắc / nút đăng nhập để tham gia | |
| ENR-02 | `student1` → khoá Spring Boot → **＋ Tham gia khoá học** | Nút đổi thành **"✓ Đã tham gia khoá học"** | |
| ENR-03 | Bấm tham gia lần nữa (nếu còn nút) / gọi lại API | Không tạo đăng ký trùng | |
| ENR-04 | Khoá **không** PUBLISHED | Không có nút tham gia | |
| ENR-05 | **Huỷ đăng ký** ngay trên thẻ khoá học ở `#/courses` | Thẻ không còn nút huỷ; vào chi tiết lại thấy nút tham gia | |
| ENR-06 | Tham gia lại sau khi huỷ | Đăng ký thành công, tiến độ được tạo mới | |
| ENR-07 | `instructor1` mở khoá học | Không có nút tham gia (giảng viên không đăng ký học) | |

---

## 9. Học & tiến độ (progress-service)

Dùng `student1` đã tham gia khoá Spring Boot.

| Mã | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|
| PRG-01 | Mở chi tiết khoá đã tham gia | Thấy tiến độ ban đầu 0% | |
| PRG-02 | Bài **VIDEO** / **ARTICLE** → **Đánh dấu đã hoàn thành** | Bài được đánh dấu xong, % tiến độ tăng | |
| PRG-03 | Mở bài QUIZ → **làm bài** (`#/quiz-attempt`) → nộp | Hiện điểm, đúng/sai từng câu | |
| PRG-04 | Xem **lịch sử lượt làm** quiz | Thấy các lượt đã nộp kèm điểm | |
| PRG-05 | Làm quiz của khoá **chưa** đăng ký | Bị chặn | |
| PRG-06 | Hoàn thành hết bài học | Tiến độ **100%** | |
| PRG-07 | Huỷ đăng ký (ENR-05) rồi tham gia lại | Tiến độ cũ được lưu trữ, tiến độ mới bắt đầu lại | |

---

## 10. Đánh giá khoá học (review-rating-service)

| Mã | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|
| REV-01 | Khách xem chi tiết khoá | Thấy danh sách đánh giá + điểm trung bình | |
| REV-02 | `student1` khoá **chưa hoàn thành** | Chưa mở nút **Gửi đánh giá** (hoặc báo chưa đủ điều kiện) | |
| REV-03 | `student1` khoá đã hoàn thành 100% → **Gửi đánh giá** (sao + nội dung) | Đánh giá xuất hiện, điểm trung bình cập nhật | |
| REV-04 | Gửi đánh giá lần 2 cho cùng khoá | Bị chặn (đã có đánh giá) — chỉ sửa được | |
| REV-05 | Sửa / xoá đánh giá của mình | Thành công | |
| REV-06 | Sửa / xoá đánh giá của **người khác** (qua API) | 403 `Bạn không có quyền sửa/xóa đánh giá này` | |
| REV-07 | Bấm **Hữu ích** trên đánh giá | Số lượt hữu ích tăng | |
| REV-08 | **Báo cáo vi phạm** một đánh giá, rồi báo cáo lại | Lần 2 báo `Bạn đã gửi báo cáo vi phạm cho đánh giá này rồi` | |
| REV-09 | `instructor1` **trả lời** đánh giá | Phần trả lời hiện dưới đánh giá | |
| REV-10 | `admin1` xem báo cáo, ẩn đánh giá vi phạm | Đánh giá bị ẩn khỏi trang công khai | |

---

## 11. Quản trị (Admin)

| Mã | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|
| ADM-01 | `admin1` → **#/admin/users** | Thấy danh sách người dùng | |
| ADM-02 | Tạo người dùng mới | Tạo thành công, đăng nhập được | |
| ADM-03 | **Khoá** (disable) một tài khoản rồi đăng nhập bằng tài khoản đó | Không đăng nhập được | |
| ADM-04 | Mở khoá lại | Đăng nhập được | |
| ADM-05 | **#/admin/requests** → duyệt yêu cầu lên giảng viên (từ AUTH-09) | Người dùng có role instructor (đăng nhập lại để thấy) | |
| ADM-06 | Từ chối một yêu cầu; xử lý lại yêu cầu đã xử lý | Lần 2 báo `Yêu cầu này đã được xử lý` | |
| ADM-07 | `admin1` → **Khoá học** | Không có nút tạo / sửa khoá học (Admin không soạn nội dung) | |

---

## 12. Phân quyền API (qua Gateway, cổng 8080)

Lấy token (Git Bash):

```bash
tok() { curl -s -X POST http://localhost:8180/realms/lms/protocol/openid-connect/token -d grant_type=password -d client_id=lms-web -d username=$1 -d password=$1 | sed -E 's/.*"access_token":"([^"]+)".*/\1/'; }
```
```bash
STU=$(tok student1); INS=$(tok instructor1); ADM=$(tok admin1)
```

| Mã | Lệnh | Mong đợi | KQ |
|---|---|---|---|
| SEC-01 | `curl -i http://localhost:8080/api/v1/courses` | 200, không có khoá `DRAFT` | |
| SEC-02 | `curl -i http://localhost:8080/api/v1/instructor/courses` | 401 | |
| SEC-03 | `curl -i -H "Authorization: Bearer $STU" http://localhost:8080/api/v1/instructor/courses` | 403 | |
| SEC-04 | `curl -i -H "Authorization: Bearer $ADM" http://localhost:8080/api/v1/instructor/courses` | 403 | |
| SEC-05 | `curl -i -H "Authorization: Bearer $INS" http://localhost:8080/api/v1/instructor/courses` | 200, chỉ khoá của `instructor1`, có DRAFT | |
| SEC-06 | `curl -i -X POST -H "Authorization: Bearer $STU" -H "Content-Type: application/json" -d '{"title":"x"}' http://localhost:8080/api/v1/courses` | 403 | |
| SEC-07 | Như SEC-06 nhưng dùng `$ADM` | 403 (Admin không tạo khoá học) | |
| SEC-08 | `curl -i -X POST -H "Authorization: Bearer $STU" -H "Content-Type: application/json" -d '{"name":"x"}' http://localhost:8080/api/v1/categories` | 403 | |
| SEC-09 | `curl -i http://localhost:8080/internal/anything` | 403/404 — API nội bộ không đi qua Gateway | |
| SEC-10 | Token hết hạn / sửa 1 ký tự trong token | 401 | |

---

## 13. Lỗi đã biết khi chạy local

| Hiện tượng | Nguyên nhân / cách xử lý |
|---|---|
| Service dùng DB báo `invalid value for parameter "TimeZone": "Asia/Saigon"` | Thêm VM option `-Duser.timezone=Asia/Ho_Chi_Minh` |
| `minio-init` báo `No license is installed` | Image `aistor` cần license; dùng `backend/docker-compose.override.yml` (image `bitnamilegacy/minio`) |
| Quên mật khẩu không gửi được email | `backend/docker-compose.yml` chưa truyền `MAIL_*` vào user-service; khi chạy IntelliJ thì đặt biến `MAIL_USERNAME` / `MAIL_PASSWORD` |
| Đổi role trong Keycloak / duyệt lên giảng viên nhưng UI chưa đổi | Đăng xuất rồi đăng nhập lại để lấy token mới |
