package com.example.enrollment_service.web.dto;

import com.example.enrollment_service.domain.Enrollment;
import com.example.enrollment_service.domain.Status;

import java.io.Serializable;
import java.time.Instant;

public record CheckResponse(boolean enrolled, String status, boolean accessGranted, Instant expiresAt)
		implements Serializable {

	public static CheckResponse of(Enrollment e, Instant now) {
		boolean notExpired = e.getExpiresAt() == null || e.getExpiresAt().isAfter(now);
		boolean granted = e.getStatus() == Status.ACTIVE && e.isAccessGranted() && notExpired;
		return new CheckResponse(true, e.getStatus().name(), granted, e.getExpiresAt());
	}

	public static CheckResponse none() {
		return new CheckResponse(false, "NONE", false, null);
	}
}
