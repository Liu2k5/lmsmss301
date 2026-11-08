import { formatBytes } from './courseMeta.js'

/** Giá trị khởi tạo cho field ảnh: url = ảnh hiện tại (nếu có), file = ảnh mới chọn, removed = muốn bỏ ảnh. */
export function initialImageValue(url) {
  return { url: url || null, file: null, removed: false }
}

/** Kiểm tra file ảnh; trả về chuỗi lỗi hoặc null. */
export function validateImage(value, maxBytes) {
  const file = value?.file
  if (!file) return null
  if (!file.type.startsWith('image/')) return 'Ảnh bìa phải là file hình ảnh (JPG, PNG, WEBP…).'
  if (maxBytes && file.size > maxBytes) return `Ảnh bìa tối đa ${formatBytes(maxBytes)}.`
  return null
}
