package com.example.progress_service.event;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentEnrolledEvent {

    private String eventId;

    private Integer studentId;

    private Integer courseId;

    private List<Integer> lessonIds;
}