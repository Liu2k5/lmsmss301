package com.example.progress_service.service;

import java.util.List;

public interface CourseServiceClient {

    List<Integer> getRequiredQuizIds(Integer courseId);
}