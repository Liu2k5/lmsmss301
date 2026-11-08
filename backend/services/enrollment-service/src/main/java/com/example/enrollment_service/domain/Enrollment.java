package com.example.enrollment_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "enrollments")
@Getter
@Setter
@NoArgsConstructor
public class Enrollment {

	@Id
	private UUID id;

	@Column(nullable = false)
	private UUID studentId;

	@Column(nullable = false)
	private UUID courseId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status;

	@Column(nullable = false)
	private Instant enrolledAt;

	private Instant completedAt;

	private Instant cancelledAt;

	private Instant expiresAt;

	@Column(nullable = false)
	private boolean accessGranted;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	private Instant deletedAt;
}
