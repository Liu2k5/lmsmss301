package com.example.course_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.course_service.dto.CategoryRequest;
import com.example.course_service.dto.CategoryResponse;
import com.example.course_service.service.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

	private final CategoryService categoryService;

	@GetMapping
	public List<CategoryResponse> findAll() {
		return categoryService.findAll();
	}

	@GetMapping("/{id}")
	public CategoryResponse findById(@PathVariable UUID id) {
		return categoryService.findById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
		return categoryService.create(request);
	}

	@PutMapping("/{id}")
	public CategoryResponse update(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
		return categoryService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID id) {
		categoryService.delete(id);
	}
}
