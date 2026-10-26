package com.example.course_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.course_service.entity.Section;

public interface SectionRepository extends JpaRepository<Section, Integer> {
}
