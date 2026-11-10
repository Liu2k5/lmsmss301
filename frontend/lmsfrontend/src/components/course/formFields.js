import { LESSON_TYPES, MATERIAL_TYPES } from './courseMeta.js'

export const COURSE_FIELDS = [
  { name: 'title', label: 'Tên khoá học', type: 'text', required: true, maxLength: 255, wide: true,
    placeholder: 'Ví dụ: Lập trình Java Spring Boot' },
  { name: 'description', label: 'Mô tả', type: 'textarea', maxLength: 10000, wide: true },
  { name: 'thumbnail', label: 'Ảnh bìa', type: 'image', wide: true, maxBytes: 5 * 1024 * 1024,
    hint: 'JPG, PNG, WEBP… tối đa 5 MB. Ảnh được lưu trên MinIO.' },
]

/** Field khoá học kèm ô chọn danh mục (danh sách danh mục tải từ course-service). */
export function courseFields(categories = []) {
  const categoryField = {
    name: 'categoryId', label: 'Danh mục', type: 'select', wide: true,
    options: [
      { value: '', label: '— Chưa chọn danh mục —' },
      ...categories.map((category) => ({ value: category.id, label: category.name })),
    ],
    hint: categories.length ? undefined : 'Chưa có danh mục nào. Admin tạo danh mục ở trang Quản trị → Danh mục.',
  }
  // Đặt ô danh mục ngay sau tên khoá học
  return [COURSE_FIELDS[0], categoryField, ...COURSE_FIELDS.slice(1)]
}

export const SECTION_FIELDS = [
  { name: 'title', label: 'Tên chương', type: 'text', required: true, maxLength: 255, wide: true,
    placeholder: 'Ví dụ: Chương 1: Tổng quan' },
  { name: 'description', label: 'Mô tả', type: 'textarea', maxLength: 5000, wide: true },
]

export const LESSON_FIELDS = [
  { name: 'title', label: 'Tên bài học', type: 'text', required: true, maxLength: 255, wide: true },
  { name: 'type', label: 'Loại bài học', type: 'select', required: true,
    options: LESSON_TYPES.map(({ value, label }) => ({ value, label })),
    hint: 'Bài học loại Quiz cần được tạo quiz ở trang Quiz.' },
  { name: 'preview', label: 'Cho xem thử miễn phí', type: 'checkbox',
    hint: 'Học viên chưa đăng ký vẫn xem được bài này.' },
  { name: 'description', label: 'Mô tả', type: 'textarea', maxLength: 5000, wide: true },
]

/** Form thêm bài học mới: thêm ô link để gắn luôn học liệu đầu tiên (không bắt buộc). */
export const NEW_LESSON_FIELDS = [
  ...LESSON_FIELDS,
  { name: 'materialUrl', label: 'Video / link học liệu (tuỳ chọn)', type: 'url', maxLength: 1000, wide: true,
    placeholder: 'https://www.youtube.com/watch?v=…',
    hint: 'Dán link video hoặc tài liệu để gắn luôn vào bài học. Muốn tải file từ máy lên thì sau khi tạo bài học, '
      + 'bấm “⬆ Upload video / file”. Bài học loại Quiz không cần link.' },
]

/** Học liệu đã upload lên MinIO: chỉ sửa tên và loại, file giữ nguyên. */
export const UPLOADED_MATERIAL_FIELDS = [
  { name: 'name', label: 'Tên học liệu', type: 'text', required: true, maxLength: 255, wide: true },
  { name: 'type', label: 'Loại', type: 'select', required: true, options: MATERIAL_TYPES,
    hint: 'File đã upload được giữ nguyên. Muốn thay file hãy xoá rồi upload lại.' },
]

export const MATERIAL_FIELDS = [
  { name: 'name', label: 'Tên học liệu', type: 'text', required: true, maxLength: 255, wide: true },
  { name: 'type', label: 'Loại', type: 'select', required: true, options: MATERIAL_TYPES },
  { name: 'mimeType', label: 'MIME type', type: 'text', maxLength: 100, placeholder: 'application/pdf' },
  { name: 'url', label: 'URL', type: 'url', required: true, maxLength: 1000, wide: true,
    placeholder: 'https://…', hint: 'Link tới file đã upload (Cloudinary / MinIO / Drive…).' },
  { name: 'fileSize', label: 'Dung lượng (byte)', type: 'number' },
]
