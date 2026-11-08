package com.example.review_rating_service;

import com.example.review_rating_service.dto.CreateReplyRequest;
import com.example.review_rating_service.dto.CreateReviewRequest;
import com.example.review_rating_service.dto.UpdateReviewStatusRequest;
import com.example.review_rating_service.entity.ReviewStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mock")
class ReviewControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String STUDENT_UUID_1 = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String STUDENT_UUID_2 = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final String STUDENT_UUID_3 = "cccccccc-cccc-cccc-cccc-cccccccccccc";
    private static final String STUDENT_UUID_4 = "dddddddd-dddd-dddd-dddd-dddddddddddd";
    private static final String STUDENT_UUID_5 = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee";
    private static final String INSTRUCTOR_UUID = "ffffffff-ffff-ffff-ffff-ffffffffffff";
    private static final String ADMIN_UUID = "12345678-1234-1234-1234-123456789012";

    @Test
    @DisplayName("Public API: Xem danh sách review không cần đăng nhập")
    void testGetCourseReviewsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/courses/1/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Public API: Xem summary điểm & phân bố sao không cần đăng nhập")
    void testGetRatingSummaryPublic() throws Exception {
        mockMvc.perform(get("/api/v1/courses/1/ratings/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(1))
                .andExpect(jsonPath("$.averageRating").exists())
                .andExpect(jsonPath("$.totalReviews").exists())
                .andExpect(jsonPath("$.starPercentages").isMap());
    }

    @Test
    @DisplayName("Bảo mật: Tạo review khi chưa đăng nhập (thiếu X-User-Id) -> 401 Unauthorized")
    void testCreateReviewUnauthenticated() throws Exception {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .rating(5)
                .comment("Đánh giá không đăng nhập")
                .build();

        mockMvc.perform(post("/api/v1/courses/1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Validation: Tạo review với điểm rating > 5 -> 400 Bad Request")
    void testCreateReviewInvalidRating() throws Exception {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .rating(6) // Vượt quá 5 sao
                .comment("Rating quá giới hạn")
                .build();

        mockMvc.perform(post("/api/v1/courses/1/reviews")
                        .header("X-User-Id", STUDENT_UUID_1)
                        .header("X-User-Role", "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.rating").exists());
    }

    @Test
    @DisplayName("UC-R01: Học viên tạo đánh giá thành công với UUID")
    void testCreateReviewSuccess() throws Exception {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .rating(5)
                .comment("Bài giảng rất chất lượng và dễ hiểu!")
                .build();

        mockMvc.perform(post("/api/v1/courses/99/reviews")
                        .header("X-User-Id", STUDENT_UUID_1)
                        .header("X-User-Role", "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseId").value(99))
                .andExpect(jsonPath("$.studentId").value(STUDENT_UUID_1))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("Chống review trùng: Học viên đã đánh giá khóa học gửi lại -> 409 Conflict")
    void testCreateDuplicateReviewConflict() throws Exception {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .rating(4)
                .comment("Đánh giá lần đầu")
                .build();

        // Lần 1: Thành công
        mockMvc.perform(post("/api/v1/courses/100/reviews")
                        .header("X-User-Id", STUDENT_UUID_2)
                        .header("X-User-Role", "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Lần 2: 409 Conflict
        mockMvc.perform(post("/api/v1/courses/100/reviews")
                        .header("X-User-Id", STUDENT_UUID_2)
                        .header("X-User-Role", "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    @DisplayName("UC-R07: Toggle Helpful Vote (Like rồi bấm lại là Unlike)")
    void testToggleHelpfulVote() throws Exception {
        // Tạo 1 review trước
        String createRes = mockMvc.perform(post("/api/v1/courses/200/reviews")
                        .header("X-User-Id", STUDENT_UUID_3)
                        .header("X-User-Role", "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateReviewRequest.builder().rating(5).comment("Hay").build())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number reviewId = objectMapper.readTree(createRes).get("id").asLong();

        // Vote lần 1 -> voted: true, helpfulCount: 1
        mockMvc.perform(post("/api/v1/reviews/" + reviewId + "/helpful")
                        .header("X-User-Id", STUDENT_UUID_4)
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voted").value(true))
                .andExpect(jsonPath("$.helpfulCount").value(1));

        // Vote lần 2 -> voted: false, helpfulCount: 0
        mockMvc.perform(post("/api/v1/reviews/" + reviewId + "/helpful")
                        .header("X-User-Id", STUDENT_UUID_4)
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voted").value(false))
                .andExpect(jsonPath("$.helpfulCount").value(0));
    }

    @Test
    @DisplayName("UC-R06: Giảng viên phản hồi đánh giá của học viên với UUID")
    void testInstructorReplyToReview() throws Exception {
        String createRes = mockMvc.perform(post("/api/v1/courses/300/reviews")
                        .header("X-User-Id", STUDENT_UUID_5)
                        .header("X-User-Role", "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateReviewRequest.builder().rating(5).comment("Khoa hoc tuyet").build())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number reviewId = objectMapper.readTree(createRes).get("id").asLong();

        CreateReplyRequest replyRequest = CreateReplyRequest.builder()
                .content("Cảm ơn bạn rất nhiều!")
                .build();

        mockMvc.perform(post("/api/v1/reviews/" + reviewId + "/replies")
                        .header("X-User-Id", INSTRUCTOR_UUID)
                        .header("X-User-Role", "INSTRUCTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.instructorId").value(INSTRUCTOR_UUID))
                .andExpect(jsonPath("$.content").value("Cảm ơn bạn rất nhiều!"));
    }

    @Test
    @DisplayName("UC-R09: Admin kiểm duyệt / ẩn đánh giá")
    void testAdminModerationHideReview() throws Exception {
        String createRes = mockMvc.perform(post("/api/v1/courses/400/reviews")
                        .header("X-User-Id", STUDENT_UUID_1)
                        .header("X-User-Role", "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateReviewRequest.builder().rating(1).comment("Spam link ...").build())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number reviewId = objectMapper.readTree(createRes).get("id").asLong();

        // Học viên không có quyền vào admin API -> 403 Forbidden
        mockMvc.perform(patch("/api/v1/admin/reviews/" + reviewId + "/status")
                        .header("X-User-Id", STUDENT_UUID_1)
                        .header("X-User-Role", "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateReviewStatusRequest(ReviewStatus.HIDDEN))))
                .andExpect(status().isForbidden());

        // Admin cập nhật trạng thái -> 200 OK
        mockMvc.perform(patch("/api/v1/admin/reviews/" + reviewId + "/status")
                        .header("X-User-Id", ADMIN_UUID)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateReviewStatusRequest(ReviewStatus.HIDDEN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HIDDEN"));
    }
}
