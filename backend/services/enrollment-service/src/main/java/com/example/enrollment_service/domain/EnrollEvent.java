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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "enroll_events")
@Getter
@Setter
@NoArgsConstructor
public class EnrollEvent {

	@Id
	private UUID id;

	@Column(nullable = false)
	private UUID enrollmentId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private EventType eventType;

	@JdbcTypeCode(SqlTypes.JSON)
	private String eventData;

	@Column(nullable = false)
	private Instant createdAt;
}
