package com.example.progress_service.controller;

import java.util.UUID;
import com.example.progress_service.entity.CourseCertificate;
import com.example.progress_service.service.CertificateService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @PostMapping("/{progressId}/issue")
    public CourseCertificate issueCertificate(
            @PathVariable UUID progressId) {

        return certificateService.issueCertificate(progressId);
    }
}