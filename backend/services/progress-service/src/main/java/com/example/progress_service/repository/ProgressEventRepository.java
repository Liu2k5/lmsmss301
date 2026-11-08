package com.example.progress_service.repository;

import java.util.UUID;
import com.example.progress_service.entity.ProgressEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressEventRepository
        extends JpaRepository<ProgressEvent, UUID> {

    boolean existsByEventId(String eventId);
}