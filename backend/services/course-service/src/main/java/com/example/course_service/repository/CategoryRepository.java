package com.example.course_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.course_service.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

	/** Danh sách category sắp theo tên A → Z. */
	List<Category> findAllByOrderByNameAsc();

	/** Dùng khi tạo mới: đã có category trùng tên chưa (không phân biệt hoa thường). */
	boolean existsByNameIgnoreCase(String name);

	/** Dùng khi sửa: có category KHÁC (id khác) trùng tên không. */
	boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

	/** Dùng cho seeder: lấy category theo tên nếu đã có. */
	Optional<Category> findByNameIgnoreCase(String name);
}
