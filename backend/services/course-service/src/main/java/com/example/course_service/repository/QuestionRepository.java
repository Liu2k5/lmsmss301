package com.example.course_service.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.course_service.entity.Question;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
}
