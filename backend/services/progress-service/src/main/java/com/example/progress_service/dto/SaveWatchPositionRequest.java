package com.example.progress_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveWatchPositionRequest {

    private Integer studentId;

    // Vị trí hiện tại của video, tính bằng giây
    private Integer watchPosition;

    // Tổng thời lượng video, tính bằng giây
    private Integer videoDuration;
}