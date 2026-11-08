package com.example.progress_service.event;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentEnrolledEvent {

    private String eventId;

    private UUID studentId;

    private UUID courseId;

    private List<UUID> lessonIds;
}