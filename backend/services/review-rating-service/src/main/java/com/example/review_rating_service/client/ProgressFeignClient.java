package com.example.review_rating_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "progress-service", fallback = ProgressFeignFallback.class)
public interface ProgressFeignClient {

    @GetMapping("/api/v1/progress/courses/{courseId}")
    ProgressStatusDto getCourseProgress(@PathVariable("courseId") UUID courseId,
                                        @RequestParam("studentId") UUID studentId);
}
