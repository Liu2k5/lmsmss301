package com.example.enrollment_service.repository;

import com.example.enrollment_service.domain.EnrollEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EnrollEventRepository extends JpaRepository<EnrollEvent, UUID> {
}
