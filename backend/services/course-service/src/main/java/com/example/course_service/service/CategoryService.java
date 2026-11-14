package com.example.course_service.service;

import java.util.List;
import java.util.UUID;

import com.example.course_service.dto.CategoryRequest;
import com.example.course_service.dto.CategoryResponse;

/** Quản lý danh mục khoá học. Xem: công khai. Tạo / sửa / xoá: chỉ Admin (chặn ở SecurityConfig). */
public interface CategoryService {

	/** Tất cả category, sắp theo tên, kèm số khoá học trong mỗi category. */
	List<CategoryResponse> findAll();

	CategoryResponse findById(UUID id);

	CategoryResponse create(CategoryRequest request);

	CategoryResponse update(UUID id, CategoryRequest request);

	/** Không cho xoá category còn khoá học. */
	void delete(UUID id);
}
