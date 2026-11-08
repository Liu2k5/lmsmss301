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
@Table(name = "enrollment_access")
@Getter
@Setter
@NoArgsConstructor
public class EnrollmentAccess {

	@Id
	private UUID id;

	@Column(nullable = false)
	private UUID enrollmentId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AccessType accessType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AccessStatus status;

	@Column(nullable = false)
	private Instant grantedAt;

	private Instant revokedAt;

	@Column(length = 255)
	private String reason;
}
