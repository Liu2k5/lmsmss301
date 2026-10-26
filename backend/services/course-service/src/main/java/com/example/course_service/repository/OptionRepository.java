package com.example.course_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.course_service.entity.Option;

public interface OptionRepository extends JpaRepository<Option, Integer> {
}
