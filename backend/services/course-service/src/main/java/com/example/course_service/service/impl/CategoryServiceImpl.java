package com.example.course_service.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.course_service.dto.CategoryCountDto;
import com.example.course_service.dto.CategoryRequest;
import com.example.course_service.dto.CategoryResponse;
import com.example.course_service.entity.Category;
import com.example.course_service.exception.ConflictException;
import com.example.course_service.exception.ResourceNotFoundException;
import com.example.course_service.repository.CategoryRepository;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.service.CategoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

	private final CategoryRepository categoryRepository;
	private final CourseRepository courseRepository;

	@Override
	public List<CategoryResponse> findAll() {
		// 1. Đếm số khoá học của từng category bằng 1 câu query, rồi đưa vào Map để tra theo id
		Map<UUID, Long> courseCounts = new HashMap<>();
		for (CategoryCountDto row : courseRepository.countByCategory()) {
			courseCounts.put(row.categoryId(), row.total());
		}

		// 2. Lấy danh sách category và ghép số khoá học vào
		List<CategoryResponse> result = new ArrayList<>();
		for (Category category : categoryRepository.findAllByOrderByNameAsc()) {
			Long total = courseCounts.get(category.getId());
			if (total == null) {
				// Category chưa có khoá học nào thì không có trong kết quả group by
				total = 0L;
			}
			result.add(toResponse(category, total));
		}
		return result;
	}

	@Override
	public CategoryResponse findById(UUID id) {
		Category category = requireCategory(id);
		long total = courseRepository.countByCategoryId(id);
		return toResponse(category, total);
	}

	@Override
	@Transactional
	public CategoryResponse create(CategoryRequest request) {
		String name = request.name().trim();

		// Tên không được trùng, so sánh không phân biệt hoa thường ("Java" trùng "java")
		if (categoryRepository.existsByNameIgnoreCase(name)) {
			throw new ConflictException("Danh mục '" + name + "' đã tồn tại");
		}

		Category category = Category.builder()
				.name(name)
				.description(trimToNull(request.description()))
				.build();
		Category saved = categoryRepository.saveAndFlush(category);
		return toResponse(saved, 0L);
	}

	@Override
	@Transactional
	public CategoryResponse update(UUID id, CategoryRequest request) {
		Category category = requireCategory(id);
		String name = request.name().trim();

		// Trùng tên với một category KHÁC thì không cho sửa (giữ nguyên tên của chính nó thì được)
		if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
			throw new ConflictException("Danh mục '" + name + "' đã tồn tại");
		}

		// Sửa trên entity có sẵn (không tạo mới) để giữ nguyên id và createdAt
		category.setName(name);
		category.setDescription(trimToNull(request.description()));
		Category saved = categoryRepository.saveAndFlush(category);

		long total = courseRepository.countByCategoryId(id);
		return toResponse(saved, total);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		Category category = requireCategory(id);

		// Còn khoá học thuộc category này thì không cho xoá, tránh khoá học bị "mất" danh mục
		long total = courseRepository.countByCategoryId(id);
		if (total > 0) {
			throw new ConflictException("Danh mục '" + category.getName() + "' đang có " + total
					+ " khoá học, hãy chuyển các khoá học sang danh mục khác trước khi xoá");
		}
		categoryRepository.delete(category);
	}

	// ================================================================ hàm phụ

	private Category requireCategory(UUID id) {
		Optional<Category> found = categoryRepository.findById(id);
		if (found.isEmpty()) {
			throw new ResourceNotFoundException("Không tìm thấy danh mục id=" + id);
		}
		return found.get();
	}

	private CategoryResponse toResponse(Category category, long courseCount) {
		return new CategoryResponse(
				category.getId(),
				category.getName(),
				category.getDescription(),
				courseCount,
				category.getCreatedAt(),
				category.getUpdatedAt());
	}

	/** Chuỗi rỗng / toàn khoảng trắng → null, ngược lại bỏ khoảng trắng đầu cuối. */
	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		return trimmed;
	}
}
