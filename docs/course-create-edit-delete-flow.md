# Luồng tạo, sửa và xoá khoá học

Tài liệu giải thích các hàm tham gia vào 3 luồng **tạo**, **sửa** và **xoá**, đi từ giao diện React xuống `course-service` (Spring Boot). Phạm vi gồm khoá học (mục 3–5) và nội dung bên trong: chương, bài học, học liệu, trong đó có upload và thay file video (mục 6–8).

> Tất cả đường dẫn file đều tính từ gốc repo.
> Backend: `backend/services/course-service/src/main/java/com/example/course_service/`
> Frontend: `frontend/lmsfrontend/src/`

---

## 1. Tổng quan

| Thao tác | Frontend gọi | API | Service | Ai được làm |
|---|---|---|---|---|
| Tạo khoá học | `CoursePage.openCreate()` | `POST /api/v1/courses` | `CourseServiceImpl.create()` | Giảng viên (INSTRUCTOR) |
| Upload ảnh bìa | `courseApi.uploadThumbnail()` | `POST /api/v1/courses/{id}/thumbnail` | `uploadThumbnail()` | Giảng viên phụ trách |
| Sửa thông tin | `CourseDetail.editCourse()` | `PUT /api/v1/courses/{id}` | `CourseServiceImpl.update()` | Giảng viên phụ trách |
| Bỏ ảnh bìa | `courseApi.removeThumbnail()` | `DELETE /api/v1/courses/{id}/thumbnail` | `removeThumbnail()` | Giảng viên phụ trách |
| Xoá khoá học | `CourseDetail.deleteCourse()` | `DELETE /api/v1/courses/{id}` | `CourseServiceImpl.delete()` | Giảng viên phụ trách, chỉ khi khoá ở `DRAFT` hoặc `ARCHIVED` |

Mỗi request ghi đi qua các tầng sau:

```
React (CoursePage / CourseDetail)
  → courseApi.js  (gắn Bearer token)
  → SecurityConfig        (chặn theo role: phải là INSTRUCTOR)
  → CourseController      (validate @Valid, lấy LoginUser từ JWT)
  → CourseServiceImpl     (kiểm tra chủ khoá, trạng thái, trùng tên → ghi DB)
  → CourseRepository / SectionRepository / LessonRepository / ...
  → GlobalExceptionHandler (đổi exception thành mã HTTP + message)
```

---

## 2. Các thành phần dùng chung

### 2.1 Phân quyền theo role: `config/SecurityConfig.java`

Mọi request `POST`, `PUT`, `PATCH`, `DELETE` tới `/api/v1/courses/**` đều yêu cầu `hasRole("INSTRUCTOR")`. Người chưa đăng nhập nhận **401**; admin hoặc học viên nhận **403**. Lớp này chỉ kiểm tra **role**, chưa kiểm tra người gọi có phải chủ khoá học hay không. Việc đó do service làm (mục 2.3).

### 2.2 `LoginUser` (`security/LoginUser.java`)

Một record nhỏ chứa thông tin người gọi lấy từ JWT.

| Hàm | Ý nghĩa |
|---|---|
| `LoginUser.from(JwtAuthenticationToken auth)` | Đọc `sub` (id người dùng) và role trong token. Token là `null` thì trả về khách (`id = null`). |
| `isGuest()` | `true` nếu `id == null`, tức chưa đăng nhập. |
| `canManage(UUID ownerId)` | `true` khi người gọi **là giảng viên** và **id trùng với `instructorId` của khoá học**. |

### 2.3 Hàm kiểm tra trong `CourseServiceImpl`

| Hàm | Làm gì | Lỗi ném ra → HTTP |
|---|---|---|
| `requireCourse(id)` | `courseRepository.findById(id)`, không có thì ném lỗi. | `ResourceNotFoundException` → **404** |
| `checkCanManage(user, course)` | Gọi `user.canManage(course.getInstructorId())`. | `AccessDeniedException` → **403** |
| `requireManageableCourse(user, id)` | Gộp 2 hàm trên: tìm khoá học rồi kiểm tra quyền chủ khoá. Mọi thao tác sửa/xoá đều bắt đầu bằng hàm này. | 404 / 403 |
| `isEditable(status)` | `status != ARCHIVED`. | |
| `ensureEditable(course)` | Khoá đã lưu trữ (`ARCHIVED`) thì không cho sửa. | `BusinessRuleException` → **400** |
| `findCategoryOrNull(categoryId)` | `categoryId` là `null` thì khoá không có danh mục. Có id mà không tìm thấy thì báo lỗi. | 404 |
| `trimToNull(value)` | Cắt khoảng trắng hai đầu; chuỗi rỗng thì thành `null` để không lưu `""` vào DB. | |
| `toDetail(course, true)` | Đổi entity thành `CourseDetailResponse` (gồm chương, bài học, học liệu, `allowedTransitions`, `editable`) để trả về cho frontend. | |

### 2.4 Validate dữ liệu đầu vào: `dto/CourseRequest.java`

```java
public record CourseRequest(
    @NotBlank @Size(max = 255)              String title,        // bắt buộc
    @Size(max = 10000)                      String description,
    @Size(max = 500) @Pattern("^$|^https?://.+") String thumbnailUrl,
    UUID categoryId)                                              // null = chưa có danh mục
```

Controller dùng `@Valid` nên dữ liệu sai được chặn **trước khi vào service**. `GlobalExceptionHandler` bắt `MethodArgumentNotValidException` và trả **400** kèm danh sách lỗi theo từng field.

### 2.5 Đổi exception thành mã HTTP: `exception/GlobalExceptionHandler.java`

| Exception | HTTP |
|---|---|
| `ResourceNotFoundException` | 404 |
| `BusinessRuleException` | 400 |
| `ConflictException` | 409 |
| `AccessDeniedException` | 403 |
| `DataIntegrityViolationException` (vi phạm ràng buộc DB) | 409 |
| `MethodArgumentNotValidException` (`@Valid` thất bại) | 400 |

### 2.6 File ảnh trên MinIO

Ảnh bìa và học liệu upload được lưu trong DB dưới dạng `"minio:<objectKey>"` (hằng `STORED_PREFIX`). Còn nếu là link ngoài thì lưu nguyên URL.

| Hàm | Làm gì |
|---|---|
| `storedKey(value)` | Giá trị bắt đầu bằng `minio:` thì trả về `objectKey`, ngược lại trả `null` (vì là link ngoài, không có gì để xoá). |
| `replaceThumbnail(course, newValue)` | Gán ảnh bìa mới. Nếu ảnh cũ là file trên MinIO và khác ảnh mới thì lên lịch xoá file cũ. |
| `deleteStoredFilesAfterCommit(keys)` | Đăng ký `TransactionSynchronization.afterCommit()`: **chỉ xoá file sau khi transaction DB commit thành công**. Nếu DB rollback thì file vẫn còn, dữ liệu không bị lệch. |
| `deleteStoredFilesQuietly(keys)` | Xoá từng file. Lỗi chỉ ghi log `warn`, không ném ra ngoài, vì DB đã commit rồi. |
| `saveOrDiscardUpload(stored, save)` | Dùng khi upload: lưu DB trong một transaction ngắn. Nếu lưu DB lỗi thì **xoá file vừa upload** để không để lại file rác. |

---

## 3. Luồng TẠO khoá học

### 3.1 Frontend

**`pages/CoursePage.jsx`, `openCreate()`**

Nút "Tạo khoá học" chỉ hiện khi `canManage = authenticated && isInstructor`. Bấm nút sẽ mở `FormModal` với:
- `fields: courseFields(categories)`: tên, danh mục, mô tả, ảnh bìa (định nghĩa ở `components/course/formFields.js`).
- `onSubmit` chạy 2 bước:

```js
onSubmit: async ({ thumbnail, ...info }) => {
  const created = await courseApi.create(info)          // (1) tạo khoá, chưa có ảnh
  setDialog(null)
  if (thumbnail?.file) {
    try {
      await courseApi.uploadThumbnail(created.id, thumbnail.file)  // (2) upload ảnh
    } catch (error) {
      notify('error', `Đã tạo khoá học nhưng chưa lưu được ảnh bìa: ...`)
      onOpenCourse(created.id); return
    }
  }
  notify('success', ...); onOpenCourse(created.id)       // chuyển sang trang chi tiết
}
```

- Ảnh bìa phải upload **sau** khi tạo, vì đường dẫn MinIO cần `courseId` (`courses/{id}/thumbnail`).
- Upload ảnh lỗi thì khoá học **vẫn được giữ**, chỉ báo lỗi riêng cho phần ảnh.
- `FormModal` tự kiểm tra trường bắt buộc và độ dài, đồng thời đổi ô trống thành `null` trước khi gửi.

**`api/courseApi.js`**
- `create: (payload) => courseRequest('/courses', json('POST', payload))`
- `courseRequest()` lấy token từ `tokenProvider` (do `AuthProvider` đăng ký) rồi gắn header `Authorization: Bearer ...`.

### 3.2 Backend

**`CourseController.create()`**
```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)   // trả 201
public CourseDetailResponse create(JwtAuthenticationToken auth, @Valid @RequestBody CourseRequest request) {
    return courseService.create(LoginUser.from(auth), request);
}
```

**`CourseServiceImpl.create()`** (`@Transactional`)
1. `instructorId = user.id()`: khoá học **luôn đứng tên người đang đăng nhập**. Frontend có gửi `instructorId` thì cũng bị bỏ qua.
2. `title = request.title().trim()`.
3. Kiểm tra trùng tên: `courseRepository.existsByInstructorIdAndTitleIgnoreCase(instructorId, title)`. Trùng thì ném `ConflictException` → **409** "Bạn đã có khoá học tên '...'". Tên chỉ cần khác nhau **giữa các khoá của cùng một giảng viên**, không phân biệt hoa/thường.
4. Tạo entity bằng `Course.builder()`:
   - `description`, `thumbnailUrl` đi qua `trimToNull()`
   - `category` lấy từ `findCategoryOrNull(request.categoryId())`
   - `status = DRAFT`: **khoá mới luôn là bản nháp**
5. `courseRepository.saveAndFlush(course)`. `@CreationTimestamp` và `@UpdateTimestamp` tự gán `createdAt` và `updatedAt`.
6. Trả về `toDetail(course, true)`.

**Upload ảnh bìa: `uploadThumbnail()`** (`@Transactional(propagation = NOT_SUPPORTED)`)

Hàm này cố ý **không** mở transaction bao quanh, để không giữ kết nối DB trong lúc upload file lên MinIO (có thể mất vài giây).
1. Kiểm tra file: không rỗng, `contentType` bắt đầu bằng `image/`, kích thước ≤ 5 MB. Sai thì trả **400**.
2. `readTransaction()`: mở transaction chỉ đọc để gọi `ensureEditable(requireManageableCourse(user, id))`.
3. `storageService.upload("courses/" + id + "/thumbnail", file)` đẩy file lên MinIO.
4. `saveOrDiscardUpload(...)`: mở transaction ghi ngắn, đọc lại khoá học, `ensureEditable`, `replaceThumbnail(course, "minio:" + key)`, rồi lưu. Lưu lỗi thì xoá file vừa upload.

### 3.3 Sơ đồ

```
Giảng viên bấm "Tạo khoá học"
  │
  ▼
openCreate() ─► FormModal (validate phía client)
  │
  ▼
courseApi.create(info) ── POST /api/v1/courses ──► SecurityConfig: INSTRUCTOR?  (401/403)
                                                    │
                                                    ▼
                                         CourseController.create  (@Valid → 400)
                                                    │
                                                    ▼
                                         CourseServiceImpl.create
                                           ├─ trùng tên? → 409
                                           ├─ findCategoryOrNull → 404
                                           └─ save (status = DRAFT) → 201
  │
  ▼ (nếu có ảnh)
courseApi.uploadThumbnail ── POST /courses/{id}/thumbnail ──► uploadThumbnail
                                                                ├─ kiểm tra file → 400
                                                                ├─ chủ khoá? → 403
                                                                ├─ upload MinIO
                                                                └─ lưu DB (lỗi → xoá file)
  │
  ▼
onOpenCourse(id) → mở trang chi tiết khoá học
```

---

## 4. Luồng SỬA thông tin khoá học

### 4.1 Frontend

**`components/course/CourseDetail.jsx`**

- Nút "✎ Sửa thông tin" chỉ hiện khi `isOwner = canManage && course.instructorId === user.id`, và bị **vô hiệu hoá** khi `!course.editable` (khoá `ARCHIVED`).

**`editCourse()`**
1. Gọi `categoryApi.list()` để lấy danh mục mới nhất. Nếu lỗi thì chỉ giữ danh mục hiện tại của khoá, form vẫn mở được.
2. Mở `FormModal` với `initialValues` lấy từ khoá hiện tại.
3. `onSubmit` được bọc bởi `formSubmit(...)`:

```js
onSubmit: formSubmit(async ({ thumbnail, ...info }) => {
  // thumbnailUrl = null → backend GIỮ NGUYÊN ảnh bìa
  let detail = await courseApi.update(course.id, { ...info, thumbnailUrl: null })
  if (thumbnail?.file)        detail = await courseApi.uploadThumbnail(course.id, thumbnail.file) // đổi ảnh
  else if (thumbnail?.removed) detail = await courseApi.removeThumbnail(course.id)                // bỏ ảnh
  return detail
}, 'Đã cập nhật khoá học.')
```

**`formSubmit(action, successMessage)`**: chạy `action`, gán kết quả vào `setCourse(detail)` để giao diện cập nhật ngay mà không cần tải lại, đóng modal rồi hiện toast. Nếu lỗi, exception được **ném lại** để `FormModal` hiện lỗi ngay trong form (ví dụ 409 trùng tên).

### 4.2 Backend

**`CourseController.update()`**: `PUT /api/v1/courses/{id}` với `@Valid @RequestBody CourseRequest`.

**`CourseServiceImpl.update()`** (`@Transactional`)
1. `requireManageableCourse(user, id)`: khoá phải tồn tại (404) và người gọi là chủ khoá (403).
2. `ensureEditable(course)`: khoá `ARCHIVED` thì trả **400**, phải khôi phục về `DRAFT` trước.
3. Kiểm tra trùng tên, **trừ chính khoá này**: `existsByInstructorIdAndTitleIgnoreCaseAndIdNot(instructorId, title, id)`. Nhờ vậy giữ nguyên tên cũ thì không bị báo trùng. Trùng với khoá khác thì trả **409**.
4. Gán `title`, `description` (qua `trimToNull`), `category` (qua `findCategoryOrNull`).
5. Ảnh bìa: chỉ đổi khi `thumbnailUrl` khác `null`. Frontend luôn gửi `null`, nên `PUT` **không đụng tới ảnh**. Đổi ảnh dùng `uploadThumbnail`, bỏ ảnh dùng `removeThumbnail`.
6. `saveAndFlush`. `@UpdateTimestamp` tự cập nhật `updatedAt`, nên khoá vừa sửa sẽ lên đầu danh sách (danh sách sắp theo `updatedAt desc`).

**`removeThumbnail()`** (`@Transactional`): `requireManageableCourse` → `ensureEditable` → `replaceThumbnail(course, null)`. File cũ trên MinIO bị xoá **sau khi commit**.

### 4.3 Sửa thông tin và đổi trạng thái là hai việc riêng

`PUT /courses/{id}` **không** đổi được `status`. Đổi trạng thái (xuất bản, ẩn, lưu trữ) đi qua `PATCH /courses/{id}/status` → `changeStatus()`, theo bảng chuyển trạng thái `TRANSITIONS`:

```
DRAFT       → PUBLISHED, ARCHIVED
PUBLISHED   → UNPUBLISHED, ARCHIVED
UNPUBLISHED → PUBLISHED, DRAFT, ARCHIVED
ARCHIVED    → DRAFT
```

Muốn chuyển sang `PUBLISHED` thì phải qua `validateCurriculumComplete()`: có ít nhất 1 chương, chương nào cũng có bài học, và mọi bài loại QUIZ đều đã có quiz.

---

## 5. Luồng XOÁ khoá học

### 5.1 Frontend

**`components/course/CourseDetail.jsx`**

- Nút "Xoá khoá học" chỉ hiện khi `isOwner && DELETABLE_STATUSES.has(course.status)`, với `DELETABLE_STATUSES = new Set(['DRAFT', 'ARCHIVED'])` (`components/course/courseMeta.js`).

**`deleteCourse()`**
1. Mở `ConfirmDialog` cảnh báo: "...cùng toàn bộ chương, bài học, học liệu và quiz sẽ bị xoá vĩnh viễn."
2. Người dùng xác nhận thì gọi `courseApi.remove(course.id)` → `DELETE /api/v1/courses/{id}`.
3. Thành công: đóng dialog, hiện toast, rồi `onBack()` quay về danh sách. Lỗi: đóng dialog và hiện toast lỗi.

### 5.2 Backend

**`CourseController.delete()`**: `@DeleteMapping("/{id}")` + `@ResponseStatus(NO_CONTENT)`, trả **204** và không có body.

**`CourseServiceImpl.delete()`** (`@Transactional`)
1. `requireManageableCourse(user, id)` (404 / 403).
2. Chỉ cho xoá khi trạng thái là `DRAFT` hoặc `ARCHIVED`. Khoá đang `PUBLISHED` hoặc `UNPUBLISHED` thì trả **409** "Hãy ẩn (UNPUBLISHED) rồi lưu trữ (ARCHIVED) trước." Lý do: khoá đã xuất bản có thể đã có học viên, không được xoá thẳng.
3. Xoá dữ liệu con **theo thứ tự từ dưới lên** để không vi phạm khoá ngoại:

```java
deleteLessons(lessonRepository.findByCourseIdOrdered(id));   // học liệu, quiz, bài học
sectionRepository.deleteAll(sectionRepository.findByCourseIdOrderByPositionAscIdAsc(id)); // chương
sectionRepository.flush();
replaceThumbnail(course, null);                              // lên lịch xoá ảnh bìa trên MinIO
courseRepository.delete(course);                             // cuối cùng mới xoá khoá học
```

**`deleteLessons(lessons)`**
1. Lấy mọi `Material` của các bài học (`findByLessonIdIn`).
2. Gom `objectKey` của những học liệu là file trên MinIO (`storedKey(material)`).
3. Xoá trong DB theo thứ tự: `Material` → `Quiz` (`findByLessonIdIn`) → `Lesson`, rồi `flush()`.
4. `deleteStoredFilesAfterCommit(storedKeys)`: file học liệu chỉ bị xoá khỏi MinIO **sau khi commit**.

Toàn bộ hàm chạy trong **một transaction**. Bước nào lỗi thì mọi thay đổi DB đều rollback, và không file nào trên MinIO bị xoá.

### 5.3 Sơ đồ

```
Bấm "Xoá khoá học" (chỉ hiện khi DRAFT/ARCHIVED)
  │
  ▼
ConfirmDialog → xác nhận
  │
  ▼
courseApi.remove(id) ── DELETE /api/v1/courses/{id} ──► SecurityConfig: INSTRUCTOR?
                                                         │
                                                         ▼
                                              CourseServiceImpl.delete  [1 transaction]
                                                ├─ requireManageableCourse → 404/403
                                                ├─ status ∉ {DRAFT, ARCHIVED} → 409
                                                ├─ deleteLessons: Material → Quiz → Lesson
                                                ├─ xoá Section
                                                ├─ replaceThumbnail(null)
                                                └─ xoá Course → 204
                                                         │
                                                  (sau commit) xoá file MinIO
  │
  ▼
onBack() → về danh sách
```

---

## 6. Luồng CHƯƠNG (Section)

### 6.1 Bảng hàm

| Thao tác | Frontend (`CourseDetail.jsx`) | API | Service |
|---|---|---|---|
| Thêm chương | `addSection()` | `POST /api/v1/courses/{id}/sections` → 201 | `addSection()` |
| Sửa chương | `editSection(section)` | `PUT /api/v1/sections/{sectionId}` | `updateSection()` |
| Xoá chương | `deleteSection(section)` | `DELETE /api/v1/sections/{sectionId}` | `deleteSection()` |
| Đổi thứ tự (↑ ↓) | `moveSection(index, delta)` | `PUT /api/v1/courses/{id}/sections/order` | `reorderSections()` |

Validate đầu vào: `SectionRequest(title @NotBlank ≤255, description ≤5000)`.

### 6.2 Chi tiết

- **Thêm:** form `SECTION_FIELDS`, tên được điền sẵn là `"Chương N: "`. Backend chạy `requireManageableCourse` → `ensureEditable` → kiểm tra trùng tên chương trong cùng khoá (`existsByCourseIdAndTitleIgnoreCase`, trùng thì **409**) → lưu với `position = findMaxPosition(courseId) + 1`, tức chương mới luôn nằm cuối.
- **Sửa:** `requireEditableSection(user, sectionId)` (tìm chương kèm khoá, kiểm tra chủ khoá và khoá chưa `ARCHIVED`). Kiểm tra trùng tên thì **trừ chính chương này** (`...AndIdNot`). Chỉ đổi `title` và `description`.
- **Xoá:** `ConfirmDialog` báo số bài học sẽ mất. Backend gọi `deleteLessons(...)` cho mọi bài trong chương (học liệu → quiz → bài học, file MinIO xoá sau commit), sau đó xoá chương và gọi `renumber(...)` để đánh lại `position` 1..n cho các chương còn lại, tránh để lại lỗ hổng thứ tự.
- **Đổi thứ tự:** frontend dùng `moved(ids, index, delta)` để đổi chỗ 2 phần tử rồi gửi **toàn bộ danh sách id** theo thứ tự mới. Backend `applyOrder(...)` bắt buộc danh sách gửi lên phải không trùng id và **gồm đúng, đủ** các id đang có; sai thì trả **400**. Đúng thì gán `position = i + 1`.

---

## 7. Luồng BÀI HỌC (Lesson)

### 7.1 Bảng hàm

| Thao tác | Frontend | API | Service |
|---|---|---|---|
| Thêm bài học | `addLesson(section)` | `POST /api/v1/sections/{sectionId}/lessons` → 201 | `addLesson()` |
| Sửa bài học | `editLesson(lesson)` | `PUT /api/v1/lessons/{lessonId}` | `updateLesson()` |
| Xoá bài học | `deleteLesson(lesson)` | `DELETE /api/v1/lessons/{lessonId}` | `deleteLesson()` |
| Đổi thứ tự | `moveLesson(section, index, delta)` | `PUT /api/v1/sections/{sectionId}/lessons/order` | `reorderLessons()` |

Validate đầu vào: `LessonRequest(title @NotBlank ≤255, description ≤5000, type @NotNull [VIDEO | ARTICLE | QUIZ], preview Boolean)`.

### 7.2 Thêm bài học, có thể kèm link

Form `NEW_LESSON_FIELDS` = `LESSON_FIELDS` + ô **`materialUrl`** (không bắt buộc). `onSubmit` chạy 2 bước:

```js
let detail = await courseApi.addLesson(section.id, lessonInfo)        // (1) tạo bài, lỗi thì form hiện lỗi
if (materialUrl && lessonInfo.type !== 'QUIZ') {
  const newLesson = /* bài cuối cùng của chương trong detail */
  try {
    detail = await courseApi.addMaterial(newLesson.id, {               // (2) gắn link làm học liệu đầu tiên
      name: lessonInfo.title,
      type: lessonInfo.type === 'VIDEO' ? 'VIDEO' : 'DOCUMENT',
      url: materialUrl,
    })
  } catch (error) { materialError = error }
}
```

- Bài mới luôn nằm cuối chương (`position = max + 1`), nên frontend lấy phần tử cuối để biết id bài vừa tạo.
- Bước (2) lỗi thì bài học **vẫn được giữ**, chỉ hiện toast "Đã thêm bài học nhưng chưa gắn được link". Khi đó dùng nút "+ Thêm link" để gắn lại.
- Muốn đưa **file từ máy** lên thì phải tạo bài trước, sau đó bấm "⬆ Upload video / file" (mục 8.3).

Backend `addLesson()`: `requireEditableSection` → kiểm tra trùng tên bài **trong cùng chương** (409) → lưu với `isPreview = Boolean.TRUE.equals(preview)` và `position = max + 1`.

### 7.3 Sửa bài học

`updateLesson()`:
1. `requireEditableLesson(user, lessonId)` (404 / 403 / 400 nếu `ARCHIVED`).
2. Bài đang là **QUIZ và đã có quiz** mà đổi sang loại khác thì trả **409** "Hãy xoá quiz trước khi đổi loại bài học". Làm vậy để quiz không bị mồ côi.
3. Kiểm tra trùng tên trong chương, trừ chính bài này (409).
4. Gán `title`, `description`, `type`, `preview`.

### 7.4 Xoá bài học

- Frontend ghi rõ trong `ConfirmDialog` bài sẽ mất kèm bao nhiêu học liệu và có quiz hay không.
- Backend `deleteLesson()` gọi `deleteLessons(List.of(lesson))` (xoá Material → Quiz → Lesson, file MinIO xoá sau commit), sau đó `renumber` lại các bài còn lại trong chương.

---

## 8. Luồng HỌC LIỆU (Material): link, upload, sửa, **thay file**, xoá

Học liệu có 2 dạng, phân biệt bằng cột `url`:

| Dạng | Giá trị `url` trong DB | `MaterialResponse.uploaded` | Link trả cho frontend |
|---|---|---|---|
| Link ngoài (YouTube, Drive…) | `https://...` | `false` | giữ nguyên URL |
| File trên MinIO | `minio:courses/{courseId}/lessons/{lessonId}/...` | `true` | `storageService.viewUrl(key)`, tạo link xem mỗi lần đọc |

### 8.1 Bảng hàm

| Thao tác | Frontend | API | Service |
|---|---|---|---|
| Thêm link | `addMaterial(lesson)` | `POST /api/v1/lessons/{lessonId}/materials` → 201 | `addMaterial()` |
| Upload file mới | `uploadMaterial(lesson)` | `POST /api/v1/lessons/{lessonId}/materials/upload` (multipart) → 201 | `uploadMaterial()` |
| Sửa thông tin | `editMaterial(material)` | `PUT /api/v1/materials/{materialId}` | `updateMaterial()` |
| **Thay file (re-upload)** | `replaceMaterialFile(material)` | `POST /api/v1/materials/{materialId}/file` (multipart) | `replaceMaterialFile()` |
| Xoá | `deleteMaterial(material)` | `DELETE /api/v1/materials/{materialId}` | `deleteMaterial()` |

Validate đầu vào: `MaterialRequest(name @NotBlank ≤255, type @NotNull, url ≤1000 và phải là http(s) hoặc rỗng, fileSize ≥0, mimeType ≤100)`.

Trên giao diện: bấm "**N học liệu ▾**" của bài học (`toggle(lesson.id)`) để mở danh sách. Mỗi học liệu có 3 nút: **✎ Sửa**, **⇪ Thay file**, **🗑 Xoá**.

### 8.2 Thêm học liệu dạng link

- Form `MATERIAL_FIELDS` (tên, loại, MIME, URL bắt buộc, dung lượng). Loại mặc định theo loại bài: VIDEO thì `VIDEO`, các loại khác thì `DOCUMENT`.
- Backend: `requireEditableLesson`, URL rỗng thì trả **400** "Muốn đưa file lên hãy dùng Upload". Sau đó `applyMaterial(...)` và lưu với `position = max + 1`.

### 8.3 Upload file (video, tài liệu)

**Frontend:** `UploadModal` cho kéo thả hoặc chọn file, tự lấy tên file (bỏ đuôi) làm tên và tự đoán loại từ MIME (`guessType`). `courseApi.uploadMaterial()` dùng **`XMLHttpRequest`** thay vì `fetch` vì cần sự kiện `upload.onprogress` để vẽ thanh %. Hàm trả về `{ promise, abort }` để nút Huỷ có thể dừng upload giữa chừng.

**Backend `uploadMaterial()`** (`@Transactional(propagation = NOT_SUPPORTED)`, giống `uploadThumbnail`):
1. File rỗng thì trả **400**. Giới hạn dung lượng do Spring đặt: `spring.servlet.multipart.max-file-size = ${MAX_UPLOAD_SIZE:1GB}`.
2. `readTransaction()`: kiểm tra quyền và khoá chưa `ARCHIVED` **trước khi upload**, để không đẩy file lên MinIO vô ích.
3. `storageService.upload("courses/{courseId}/lessons/{lessonId}", file)`.
4. Tên hiển thị lấy theo thứ tự ưu tiên: `name` gửi lên → tên file gốc → `"Học liệu"`. Loại lấy theo: `type` gửi lên → `guessType(contentType)` (video/ → VIDEO, image/ → IMAGE, pdf/word/… → DOCUMENT, còn lại → OTHER).
5. `saveOrDiscardUpload(...)`: transaction ghi ngắn, lưu `Material` với `url = "minio:" + objectKey`, `fileSize`, `mimeType`. Lưu lỗi thì **xoá file vừa upload**.

### 8.4 Sửa thông tin học liệu

`editMaterial()` chọn form theo dạng học liệu:
- `uploaded = true` dùng `UPLOADED_MATERIAL_FIELDS`, **chỉ có tên và loại**. Form có gợi ý "Muốn đổi file, bấm nút ⇪ Thay file".
- `uploaded = false` dùng `MATERIAL_FIELDS` đầy đủ, sửa được cả URL.

Backend `updateMaterial()` có 3 trường hợp:

| `url` gửi lên | Học liệu hiện tại | Kết quả |
|---|---|---|
| rỗng | link | **400** "URL học liệu không được để trống" |
| rỗng | file MinIO | Chỉ sửa `name`, `type`. **File, dung lượng và MIME giữ nguyên** |
| có URL | bất kỳ | `applyMaterial` ghi đè. Nếu trước đó là file MinIO thì file cũ bị xoá **sau commit** (đổi file thành link) |

### 8.5 Thay file / re-upload video

Đây là luồng dùng khi muốn **đổi video của một bài học mà không xoá rồi tạo lại học liệu**, nên id, vị trí và tiến độ học gắn với học liệu đều được giữ.

**Frontend `replaceMaterialFile(material)`**: mở `UploadModal` với:
- `title: 'Thay file học liệu'`, `submitLabel: 'Thay file'`
- `initialName: material.name`: **giữ tên cũ**. `UploadModal.pick()` chỉ lấy tên file mới làm tên khi `initialName == null`.
- `defaultType: material.type`
- `notice` khác nhau theo dạng: file MinIO thì báo "File hiện tại trên MinIO sẽ bị xoá sau khi file mới được lưu"; link thì báo "Học liệu đang là link. Sau khi thay, học liệu sẽ dùng file bạn upload lên MinIO".
- Gọi `courseApi.replaceMaterialFile(id, file, meta, onProgress)`, cũng qua `uploadWithProgress` (XHR, có % và huỷ được).

**Backend `replaceMaterialFile()`** (`NOT_SUPPORTED`):
1. File rỗng thì trả **400**.
2. `readTransaction()`: `requireEditableMaterial` (404/403/400), đồng thời tính thư mục `courses/{courseId}/lessons/{lessonId}`.
3. Upload file mới lên MinIO.
4. `saveOrDiscardUpload(...)`:
   - đọc lại học liệu, lấy `oldStoredKey = storedKey(material)`
   - gán `url = "minio:" + newKey`, `fileSize`, `mimeType`; có `name` hoặc `type` gửi lên thì cập nhật luôn
   - `saveAndFlush`
   - nếu `oldStoredKey != null` và khác key mới thì `deleteStoredFilesAfterCommit([oldStoredKey])`

**Bảo đảm an toàn dữ liệu:**

```
upload file mới ──► lưu DB ──┬─ thành công ─► commit ─► xoá file CŨ trên MinIO
                             └─ lỗi ───────► rollback ─► xoá file MỚI, file cũ còn nguyên
```

Ở bất kỳ thời điểm nào, học liệu cũng luôn trỏ tới **một file có thật**.

### 8.6 Xoá học liệu

`deleteMaterial()`: `requireEditableMaterial` → xoá bản ghi → `flush` → file MinIO (nếu có) bị xoá **sau commit** → `renumber` lại các học liệu còn lại của bài.

### 8.7 Phản hồi chung của các API soạn nội dung

Mọi API ở mục 6–8 đều trả về **toàn bộ `CourseDetailResponse`** qua `refreshed(courseId)` (gọi `flush` rồi `toDetail`). Frontend chỉ cần `setCourse(detail)`, không phải tải lại trang:
- `run(action, msg)`: dùng cho xoá và đổi thứ tự. Bật `busy` để khoá các nút, lỗi thì hiện toast.
- `formSubmit(action, msg)`: dùng cho form thêm/sửa. Lỗi được ném lên để `FormModal` hiện ngay trong form (ví dụ 409 trùng tên).

---

## 9. Tóm tắt quy tắc nghiệp vụ

| Quy tắc | Kiểm tra ở đâu | Lỗi |
|---|---|---|
| Chỉ INSTRUCTOR được tạo/sửa/xoá | `SecurityConfig` | 401 / 403 |
| Chỉ giảng viên **phụ trách** được sửa/xoá khoá của mình | `checkCanManage` | 403 |
| Khoá mới luôn ở `DRAFT`, luôn đứng tên người tạo | `create()` | |
| Tên khoá không trùng trong các khoá của cùng giảng viên (không phân biệt hoa/thường) | `existsByInstructorIdAndTitleIgnoreCase[AndIdNot]` | 409 |
| Tên bắt buộc, ≤ 255 ký tự; mô tả ≤ 10000; URL ảnh phải là http(s) | `CourseRequest` + `@Valid` | 400 |
| Danh mục (nếu có) phải tồn tại | `findCategoryOrNull` | 404 |
| Khoá `ARCHIVED` không sửa được | `ensureEditable` | 400 |
| Ảnh bìa: file ảnh, ≤ 5 MB | `uploadThumbnail` | 400 |
| Chỉ xoá được khoá `DRAFT` hoặc `ARCHIVED` | `delete()` (UI cũng ẩn nút) | 409 |
| File MinIO chỉ bị xoá sau khi DB commit; upload lỗi khi lưu DB thì xoá file vừa upload | `deleteStoredFilesAfterCommit`, `saveOrDiscardUpload` | |
| Chương, bài học, học liệu chỉ sửa được khi khoá chưa `ARCHIVED` và người gọi là chủ khoá | `requireEditableSection/Lesson/Material` | 404 / 403 / 400 |
| Tên chương không trùng trong khoá; tên bài không trùng trong chương | `existsBy...TitleIgnoreCase[AndIdNot]` | 409 |
| Không đổi loại bài QUIZ khi đã có quiz | `updateLesson()` | 409 |
| Danh sách sắp xếp phải gồm đúng và đủ id, không trùng | `applyOrder()` | 400 |
| Xoá phần tử thì đánh lại `position` 1..n | `renumber()` | |
| Học liệu link phải có URL; sửa học liệu đã upload mà để trống URL thì giữ nguyên file | `addMaterial()`, `updateMaterial()` | 400 |
| Upload file tối đa `MAX_UPLOAD_SIZE` (mặc định 1 GB) | `application.properties` + `GlobalExceptionHandler.handleTooLarge` | 413 |
| Thay file giữ nguyên id và vị trí học liệu; file cũ xoá sau commit | `replaceMaterialFile()` | |

## 10. Test liên quan

`backend/services/course-service/src/test/java/com/example/course_service/controller/`
- `CourseControllerIntegrationTest`: tạo khoá luôn ở DRAFT, thiếu tên trả 400, cập nhật thông tin, vòng đời trạng thái (không xoá được khi PUBLISHED, xoá được khi ARCHIVED, xoá kéo theo chương và bài học).
- `CourseSecurityIntegrationTest`: phân quyền (khách, học viên, admin, giảng viên không phải chủ khoá).
- `DuplicateNameIntegrationTest`: trùng tên khoá học.
- `MaterialUploadIntegrationTest`: upload ảnh bìa và học liệu lên MinIO.
