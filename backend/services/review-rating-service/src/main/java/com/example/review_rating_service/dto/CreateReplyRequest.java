package com.example.review_rating_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReplyRequest {

    @NotBlank(message = "Nội dung phản hồi không được để trống")
    @Size(max = 2000, message = "Nội dung phản hồi tối đa 2000 ký tự")
    private String content;
}
