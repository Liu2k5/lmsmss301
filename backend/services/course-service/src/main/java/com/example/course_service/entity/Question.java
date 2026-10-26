package com.example.course_service.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "questions")
@Getter
@Setter
@AllArgsConstructor
@Builder
public class Question {

	/** Constructor rỗng cho JPA; phải tự khởi tạo collection vì @Builder.Default không áp dụng cho constructor. */
	public Question() {
		this.options = new ArrayList<>();
	}


	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quiz_id", nullable = false)
	private Quiz quiz;

	@Column(name = "content", nullable = false, columnDefinition = "text")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 20)
	private QuestionType type;

	@Column(name = "position", nullable = false)
	private Integer position;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position ASC, id ASC")
	@Builder.Default
	private List<Option> options = new ArrayList<>();

	public void addOption(Option option) {
		options.add(option);
		option.setQuestion(this);
	}

	public void removeOption(Option option) {
		// Chỉ bỏ khỏi collection; orphanRemoval sẽ DELETE. Không set question = null
		// để tránh UPDATE question_id = NULL trước DELETE.
		options.remove(option);
	}
}

