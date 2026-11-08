package com.example.progress_service.event;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentUnenrolledEvent {

    private String eventId;

    private Integer studentId;

    private Integer courseId;
}