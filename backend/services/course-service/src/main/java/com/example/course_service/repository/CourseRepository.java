package com.example.course_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.course_service.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Integer> {
}
