package com.example.review_rating_service.controller;

import com.example.review_rating_service.dto.GrantPermissionRequest;
import com.example.review_rating_service.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class InternalReviewPermissionController {

    private final ReviewService reviewService;

    /** Kích hoạt quyền đánh giá khóa học cho học viên (Dành cho Progress Service gọi) */
    @PostMapping("/api/v1/reviews/permissions")
    public ResponseEntity<Map<String, Object>> grantPermission(@Valid @RequestBody GrantPermissionRequest request) {
        reviewService.grantReviewPermission(request.getStudentId(), request.getCourseId());
        return ResponseEntity.ok(Collections.singletonMap("reviewPermissionGranted", true));
    }
}
