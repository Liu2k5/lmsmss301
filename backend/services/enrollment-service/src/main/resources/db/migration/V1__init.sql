CREATE TABLE enrollments (
  id UUID PRIMARY KEY,
  student_id UUID NOT NULL,
  course_id UUID NOT NULL,
  status VARCHAR(20) NOT NULL,
  enrolled_at TIMESTAMPTZ NOT NULL,
  completed_at TIMESTAMPTZ,
  cancelled_at TIMESTAMPTZ,
  expires_at TIMESTAMPTZ,
  access_granted BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  deleted_at TIMESTAMPTZ,
  CONSTRAINT uq_enrollment_student_course UNIQUE (student_id, course_id)
);
CREATE INDEX idx_enrollments_course ON enrollments(course_id);

CREATE TABLE enrollment_access (
  id UUID PRIMARY KEY,
  enrollment_id UUID NOT NULL REFERENCES enrollments(id),
  access_type VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  granted_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ,
  reason VARCHAR(255)
);

CREATE TABLE enroll_events (
  id UUID PRIMARY KEY,
  enrollment_id UUID NOT NULL REFERENCES enrollments(id),
  event_type VARCHAR(30) NOT NULL,
  event_data JSONB,
  created_at TIMESTAMPTZ NOT NULL
);
