package com.example.progress_service.repository;

import com.example.progress_service.entity.CourseCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CourseCertificateRepository
        extends JpaRepository<CourseCertificate, UUID> {

    Optional<CourseCertificate> findByCourseProgressId(UUID progressId);

    boolean existsByCourseProgressId(UUID progressId);

    Optional<CourseCertificate> findByCertificateCode(String code);
}