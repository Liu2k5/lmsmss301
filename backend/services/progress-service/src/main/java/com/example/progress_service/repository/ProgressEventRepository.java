package com.example.progress_service.repository;

import com.example.progress_service.entity.ProgressEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressEventRepository
        extends JpaRepository<ProgressEvent, Integer> {

    boolean existsByEventId(String eventId);
}