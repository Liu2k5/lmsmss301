package com.example.progress_service.event;

import java.util.UUID;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentUnenrolledEvent {

    private String eventId;

    private UUID studentId;

    private UUID courseId;
}