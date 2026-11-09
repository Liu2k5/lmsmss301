# Tài Liệu Kịch Bản Kiểm Thử & Giám Sát Cơ Sở Dữ Liệu (Review & Rating Service)

Tài liệu này cung cấp hướng dẫn chi tiết cách kết nối, theo dõi dữ liệu cơ sở dữ liệu PostgreSQL và toàn bộ các kịch bản kiểm thử chức năng (Test Scenarios) cho microservice **Review & Rating Service** (Port `8085`, Database `review_db`).

---

## 1. Kết Nối & Giám Sát Cơ Sở Dữ Liệu

### 1.1. Thông số kết nối PostgreSQL
| Thuộc tính | Giá trị |
|---|---|
| **DBMS** | PostgreSQL (v15+) |
| **Host** | `localhost` (hoặc `127.0.0.1`) |
| **Port** | `5432` |
| **Database** | `review_db` |
| **Username** | `postgres` |
| **Password** | `postgres` |

### 1.2. Cách theo dõi dữ liệu
1. **IntelliJ Database Tool**:
   - Mở tab **Database** ở mép phải IntelliJ IDEA ➔ Bấm **`+`** ➔ **Data Source** ➔ **PostgreSQL**.
   - Nhập thông số ở bảng trên ➔ Bấm **Test Connection** ➔ **OK**.
   - Bấm đúp vào các bảng để xem dữ liệu như bảng tính Excel.
2. **DBeaver / TablePlus / pgAdmin**:
   - Tạo New Connection chọn PostgreSQL và điền các thông số trên.
3. **Theo dõi SQL Console thời gian thực**:
   - File `application.properties` đã bật `spring.jpa.show-sql=true`. Khi gọi API, toàn bộ câu lệnh `INSERT`, `UPDATE`, `SELECT` sẽ được in định dạng đẹp mắt ngay trong cửa sổ Run Console của IntelliJ.

### 1.3. Các câu lệnh SQL truy vấn nhanh khi kiểm thử
```sql
-- 1. Xem tất cả đánh giá, trạng thái và cờ xóa mềm
SELECT id, course_id, student_id, rating, comment, status, helpful_count, deleted_at, created_at 
FROM reviews ORDER BY id DESC;

-- 2. Xem điểm trung bình và phân bố sao của từng khóa học
SELECT course_id, average_rating, total_reviews, star_5_count, star_4_count, star_3_count, star_2_count, star_1_count 
FROM course_rating_summaries;

-- 3. Xem câu trả lời của giảng viên cho đánh giá
SELECT id, review_id, instructor_id, content, created_at 
FROM review_replies;

-- 4. Xem lượt vote hữu ích (Like / Helpful)
SELECT id, review_id, user_id, created_at 
FROM review_helpful_votes;

-- 5. Xem danh sách báo cáo vi phạm
SELECT id, review_id, reporter_id, reason, status, created_at 
FROM review_reports;

-- 6. Xem quyền đánh giá (do Progress Service cấp)
SELECT id, course_id, student_id, granted_at 
FROM review_permissions;
```

---

## 2. Danh Sách UUID Dùng Trong Kiểm Thử

Hệ thống sử dụng định dạng chuẩn **UUID** cho các trường định danh người dùng:

| Vai trò | Tên biến mẫu | Giá trị UUID |
|---|---|---|
| Học viên 1 | `student101Id` | `11111111-1111-1111-1111-000000000101` |
| Học viên 2 | `student102Id` | `11111111-1111-1111-1111-000000000102` |
| Học viên mới | `student105Id` | `11111111-1111-1111-1111-000000000105` |
| Học viên xong khóa | `student108Id` | `11111111-1111-1111-1111-000000000108` |
| Giảng viên | `instructorId` | `22222222-2222-2222-2222-000000000007` |
| Quản trị viên (Admin) | `adminId` | `33333333-3333-3333-3333-000000000001` |

> File chạy trực tiếp: `backend/services/review-rating-service/review-rating-service.http`

---

## 3. Các Kịch Bản Kiểm Thử Chức Năng (Test Scenarios)

### Kịch bản 1: Khởi động hệ thống & Dữ liệu Seed mẫu
* **Mục tiêu**: Đảm bảo service khởi động thành công và dữ liệu mẫu đã được nạp sẵn.
* **Request 1.1**:
  * `GET http://localhost:8085/actuator/health`
  * **Kỳ vọng**: HTTP `200 OK`, `{"status": "UP"}`.
* **Request 1.2 (UC-R05)**:
  * `GET http://localhost:8085/api/v1/courses/1/ratings/summary`
  * **Kỳ vọng**: HTTP `200 OK`, `totalReviews: 3`, `averageRating: 4.67`.
* **Quan sát Database**:
  * Bảng `reviews`: Có sẵn 4 bản ghi (3 cho khóa 1, 1 cho khóa 2).
  * Bảng `course_rating_summaries`: Khóa 1 có `average_rating = 4.67`, Khóa 2 có `average_rating = 5.00`.

---

### Kịch bản 2: Xem danh sách đánh giá & Lọc theo sao (UC-R04)
* **Mục tiêu**: Người dùng xem danh sách đánh giá công khai, sắp xếp và lọc.
* **Request 2.1 (Sắp xếp mới nhất)**:
  * `GET http://localhost:8085/api/v1/courses/1/reviews?sort=newest&page=0&size=10`
  * **Kỳ vọng**: HTTP `200 OK`, trả về danh sách có phân trang, các review mới nhất xếp đầu.
* **Request 2.2 (Lọc 5 sao)**:
  * `GET http://localhost:8085/api/v1/courses/1/reviews?rating=5`
  * **Kỳ vọng**: HTTP `200 OK`, chỉ có các review có `rating: 5`.
* **Request 2.3 (Sắp xếp nhiều người thấy hữu ích nhất)**:
  * `GET http://localhost:8085/api/v1/courses/1/reviews?sort=most_helpful`
  * **Kỳ vọng**: Review có `helpful_count = 12` xếp vị trí đầu tiên.

---

### Kịch bản 3: Kiểm tra quyền viết đánh giá (Eligibility - UC-R11)
* **Mục tiêu**: Frontend kiểm tra điều kiện học viên trước khi hiển thị nút hoặc form viết review.
* **Request**:
  * `GET http://localhost:8085/api/v1/courses/1/reviews/eligibility`
  * **Header**:
    * `X-User-Id: 11111111-1111-1111-1111-000000000105`
    * `X-User-Role: STUDENT`
  * **Kỳ vọng**: HTTP `200 OK`, `{"canReview": true, "reason": "Hợp lệ để viết đánh giá"}`.

---

### Kịch bản 4: Học viên viết đánh giá mới & Tự động tính lại điểm (UC-R01, UC-R05)
* **Mục tiêu**: Học viên gửi đánh giá mới, hệ thống tự động lưu và tính lại điểm trung bình sao cho khóa học.
* **Request**:
  * `POST http://localhost:8085/api/v1/courses/1/reviews`
  * **Header**:
    * `X-User-Id: 11111111-1111-1111-1111-000000000105`
    * `X-User-Role: STUDENT`
    * `Content-Type: application/json`
  * **Body**:
    ```json
    {
      "rating": 5,
      "comment": "Khóa học rất bài bản, ví dụ trực quan dễ hiểu!"
    }
    ```
* **Kỳ vọng API**: HTTP `201 Created`, trả về review với `status: "APPROVED"`, `helpfulCount: 0`.
* **Quan sát Database**:
  1. `SELECT * FROM reviews WHERE student_id = '11111111-1111-1111-1111-000000000105';` ➔ Thêm 1 dòng mới.
  2. `SELECT * FROM course_rating_summaries WHERE course_id = 1;` ➔ `total_reviews` tăng từ 3 lên **4**, `star_5_count` tăng thêm 1, `average_rating` được tính lại thành **4.75** `((5+4+5+5)/4 = 4.75)`.

---

### Kịch bản 5: Chặn gửi đánh giá trùng lặp (Anti-Spam Guard)
* **Mục tiêu**: Mỗi học viên chỉ được đánh giá 1 lần duy nhất cho mỗi khóa học.
* **Thực hiện**: Gửi lại request của **Kịch bản 4** với cùng `X-User-Id` trên Khóa học 1.
* **Kỳ vọng API**: HTTP `409 Conflict`, body:
  ```json
  {
    "code": "CONFLICT",
    "message": "Bạn đã gửi đánh giá cho khóa học này rồi"
  }
  ```
* **Quan sát Database**: Bảng `reviews` không sinh thêm bản ghi nào.

---

### Kịch bản 6: Chỉnh sửa đánh giá của bản thân (UC-R02)
* **Mục tiêu**: Học viên sửa lại điểm sao hoặc nhận xét của mình.
* **Request**:
  * `PUT http://localhost:8085/api/v1/reviews/{reviewId}` (ví dụ reviewId vừa tạo ở Kịch bản 4)
  * **Header**:
    * `X-User-Id: 11111111-1111-1111-1111-000000000105`
    * `X-User-Role: STUDENT`
    * `Content-Type: application/json`
  * **Body**:
    ```json
    {
      "rating": 3,
      "comment": "Nội dung tạm ổn nhưng cần bổ sung thêm bài tập thực hành."
    }
    ```
* **Kỳ vọng API**: HTTP `200 OK`, `rating: 3`.
* **Quan sát Database**:
  * Cột `rating` trong bảng `reviews` chuyển thành `3`, `updated_at` nhảy mốc thời gian mới.
  * Bảng `course_rating_summaries`: Điểm trung bình tự động tính lại thành **4.25** `((5+4+5+3)/4 = 4.25)`.

---

### Kịch bản 7: Giảng viên phản hồi đánh giá (UC-R06)
* **Mục tiêu**: Giảng viên phụ trách khóa học trả lời bài đánh giá của học viên.
* **Request**:
  * `POST http://localhost:8085/api/v1/reviews/{reviewId}/replies`
  * **Header**:
    * `X-User-Id: 22222222-2222-2222-2222-000000000007`
    * `X-User-Role: INSTRUCTOR`
    * `Content-Type: application/json`
  * **Body**:
    ```json
    {
      "content": "Cảm ơn bạn đã góp ý! Giảng viên sẽ bổ sung bài tập trong tuần tới nhé."
    }
    ```
* **Kỳ vọng API**: HTTP `201 Created`.
* **Quan sát Database**:
  * `SELECT * FROM review_replies WHERE review_id = {reviewId};` ➔ Xuất hiện dòng phản hồi liên kết với review.

---

### Kịch bản 8: Đánh dấu hữu ích (Helpful Vote Toggle - UC-R07)
* **Mục tiêu**: Học viên khác bấm Like / Hủy Like trên một đánh giá.
* **Lần 1 (Bấm Like)**:
  * `POST http://localhost:8085/api/v1/reviews/{reviewId}/helpful`
  * **Header**: `X-User-Id: 11111111-1111-1111-1111-000000000102`, `X-User-Role: STUDENT`
  * **Kỳ vọng API**: `{"voted": true, "helpfulCount": 1}`.
  * **DB**: Bảng `review_helpful_votes` thêm 1 dòng; bảng `reviews` có `helpful_count = 1`.
* **Lần 2 (Bấm lại để Hủy Like)**:
  * Gửi lại đúng request trên cùng `X-User-Id`.
  * **Kỳ vọng API**: `{"voted": false, "helpfulCount": 0}`.
  * **DB**: Bản ghi trong `review_helpful_votes` bị xóa; `helpful_count` trong `reviews` giảm về `0`.

---

### Kịch bản 9: Báo cáo vi phạm (UC-R08)
* **Mục tiêu**: Người dùng báo cáo review có dấu hiệu quảng cáo hoặc xúc phạm.
* **Request**:
  * `POST http://localhost:8085/api/v1/reviews/{reviewId}/reports`
  * **Header**:
    * `X-User-Id: 11111111-1111-1111-1111-000000000102`
    * `X-User-Role: STUDENT`
    * `Content-Type: application/json`
  * **Body**:
    ```json
    {
      "reason": "Nhận xét có dấu hiệu quảng cáo spam ngoài luồng"
    }
    ```
* **Kỳ vọng API**: HTTP `201 Created`, `status: "PENDING"`.
* **Quan sát Database**:
  * `SELECT * FROM review_reports WHERE review_id = {reviewId};` ➔ Thấy báo cáo với trạng thái `PENDING`.

---

### Kịch bản 10: Admin xem báo cáo & Ẩn đánh giá vi phạm (UC-R09)
* **Mục tiêu**: Quản trị viên duyệt báo cáo và ẩn review vi phạm khỏi người dùng.
* **Request 10.1 (Xem báo cáo)**:
  * `GET http://localhost:8085/api/v1/admin/reports?status=PENDING&page=0&size=10`
  * **Header**: `X-User-Id: 33333333-3333-3333-3333-000000000001`, `X-User-Role: ADMIN`
  * **Kỳ vọng**: HTTP `200 OK`, danh sách các báo cáo đang chờ xử lý.
* **Request 10.2 (Ẩn review)**:
  * `PATCH http://localhost:8085/api/v1/admin/reviews/{reviewId}/status`
  * **Header**:
    * `X-User-Id: 33333333-3333-3333-3333-000000000001`
    * `X-User-Role: ADMIN`
    * `Content-Type: application/json`
  * **Body**: `{"status": "HIDDEN"}`
  * **Kỳ vọng**: HTTP `200 OK`, `status: "HIDDEN"`.
* **Quan sát Database**:
  * Bảng `reviews`: Cột `status = 'HIDDEN'`.
  * Bảng `course_rating_summaries`: **Review bị ẩn sẽ lập tức bị loại ra khỏi thống kê** (tổng số review giảm đi 1, trung bình sao tính lại trên các review `APPROVED` còn lại).

---

### Kịch bản 11: Xóa mềm đánh giá (Soft-delete - UC-R03)
* **Mục tiêu**: Học viên xóa đánh giá của mình (giữ lại dữ liệu audit nhưng không hiển thị).
* **Request**:
  * `DELETE http://localhost:8085/api/v1/reviews/{reviewId}`
  * **Header**: `X-User-Id: 11111111-1111-1111-1111-000000000105`, `X-User-Role: STUDENT`
* **Kỳ vọng API**: HTTP `204 No Content`.
* **Quan sát Database**:
  * `SELECT id, deleted_at FROM reviews WHERE id = {reviewId};` ➔ Bản ghi **vẫn còn trong DB**, nhưng cột `deleted_at` được gán mốc thời gian hiện tại (`NOT NULL`).
  * Gọi lại `GET /api/v1/courses/1/reviews` ➔ Review này không còn xuất hiện trong danh sách trả về.

---

### Kịch bản 12: Tiến trình hoàn thành khóa học cấp quyền đánh giá (UC-P04 / Internal API)
* **Mục tiêu**: Kiểm tra endpoint nội bộ mà **Progress Service** gọi khi học viên đạt 100% tiến độ khóa học.
* **Request**:
  * `POST http://localhost:8085/api/v1/reviews/permissions`
  * **Header**: `Content-Type: application/json`
  * **Body**:
    ```json
    {
      "studentId": "11111111-1111-1111-1111-000000000108",
      "courseId": 1
    }
    ```
* **Kỳ vọng API**: HTTP `200 OK`, `{"reviewPermissionGranted": true}`.
* **Quan sát Database**:
  * `SELECT * FROM review_permissions WHERE student_id = '11111111-1111-1111-1111-000000000108';` ➔ Xuất hiện quyền review được cấp cho khóa học 1.
