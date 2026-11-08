package com.example.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UniversityRequest(
    @NotBlank 
    @Size(max = 100) 
    String name,

    @Size(max = 200) 
    String address
) {}