package com.example.review_rating_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "enrollment-service", url = "${app.services.enrollment.url:}", fallback = EnrollmentFeignFallback.class)
public interface EnrollmentFeignClient {

    @GetMapping("/api/v1/enrollments/check")
    EnrollmentStatusDto checkEnrollment(@RequestParam("studentId") UUID studentId,
                                        @RequestParam("courseId") UUID courseId);
}
