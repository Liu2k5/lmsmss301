package com.example.progress_service.dto;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveWatchPositionRequest {

    private UUID studentId;

    // Vị trí hiện tại của video, tính bằng giây
    private Integer watchPosition;

    // Tổng thời lượng video, tính bằng giây
    private Integer videoDuration;
}