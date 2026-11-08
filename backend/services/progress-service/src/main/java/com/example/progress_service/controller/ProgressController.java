package com.example.progress_service.controller;

import com.example.progress_service.dto.*;
import com.example.progress_service.service.*;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    private final LessonProgressService lessonProgressService;

    private final QuizProgressService quizProgressService;

    // UC-P02: Mark lesson completed
    @PutMapping("/courses/{courseId}/lessons/{lessonId}/complete")
    public String completeLesson(
            @PathVariable Integer courseId,
            @PathVariable Integer lessonId,
            @RequestBody CompleteLessonRequest request) {

        lessonProgressService.markLessonCompleted(
                request.getStudentId(),
                courseId,
                lessonId
        );

        return "Lesson completed successfully";
    }

    // UC-P03: Save video watch position
@PutMapping("/courses/{courseId}/lessons/{lessonId}/watch-position")
public String saveWatchPosition(
        @PathVariable Integer courseId,
        @PathVariable Integer lessonId,
        @RequestBody SaveWatchPositionRequest request) {

    lessonProgressService.saveWatchPosition(
            request.getStudentId(),
            courseId,
            lessonId,
            request.getWatchPosition(),
            request.getVideoDuration());

    return "Watch position saved successfully";
}

    // UC-P05: View progress
    @GetMapping("/courses/{courseId}")
    public CourseProgressResponse getProgress(
            @RequestParam Integer studentId,
            @PathVariable Integer courseId) {

        return progressService.getCourseProgress(
                studentId,
                courseId
        );
    }

    // UC-P06: Resume learning
    @GetMapping("/courses/{courseId}/resume")
    public ResumeLearningResponse resumeLearning(
            @RequestParam Integer studentId,
            @PathVariable Integer courseId) {

        return progressService.resumeLearning(
                studentId,
                courseId
        );
    }


    // UC-P07: Completation Checking
    @GetMapping("/courses/{courseId}/completion")
    public CourseProgressResponse checkCourseCompletion(
            @RequestParam Integer studentId,
            @PathVariable Integer courseId) {

        return progressService.getCourseProgress(studentId, courseId);
    }
}