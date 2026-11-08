package com.example.progress_service.repository;

import com.example.progress_service.entity.CourseCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseCertificateRepository
        extends JpaRepository<CourseCertificate, Integer> {

    Optional<CourseCertificate> findByCourseProgressId(Integer progressId);

    boolean existsByCourseProgressId(Integer progressId);

    Optional<CourseCertificate> findByCertificateCode(String code);
}