package com.example.course_service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quizzes")
@Getter
@Setter
@AllArgsConstructor
@Builder
public class Quiz {

	/** Constructor rỗng cho JPA; phải tự khởi tạo collection vì @Builder.Default không áp dụng cho constructor. */
	public Quiz() {
		this.questions = new ArrayList<>();
	}


	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Integer id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lesson_id", nullable = false, unique = true)
	private Lesson lesson;

	@Column(name = "title", nullable = false, length = 255)
	private String title;

	@Column(name = "description", columnDefinition = "text")
	private String description;

	@Column(name = "passing_score", nullable = false, precision = 5, scale = 2)
	private BigDecimal passingScore;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position ASC, id ASC")
	@Builder.Default
	private List<Question> questions = new ArrayList<>();

	public void addQuestion(Question question) {
		questions.add(question);
		question.setQuiz(this);
	}

	public void removeQuestion(Question question) {
		// Chỉ bỏ khỏi collection; orphanRemoval sẽ DELETE. Không set quiz = null vì
		// Hibernate sẽ phát sinh UPDATE quiz_id = NULL trước DELETE và vi phạm NOT NULL.
		questions.remove(question);
	}
}

