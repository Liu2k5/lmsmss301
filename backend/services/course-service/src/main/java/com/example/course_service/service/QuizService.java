package com.example.course_service.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.course_service.dto.LessonOptionResponse;
import com.example.course_service.dto.OptionRequest;
import com.example.course_service.dto.OptionResponse;
import com.example.course_service.dto.QuestionRequest;
import com.example.course_service.dto.QuestionResponse;
import com.example.course_service.dto.QuizRequest;
import com.example.course_service.dto.QuizResponse;
import com.example.course_service.dto.QuizSummaryResponse;
import com.example.course_service.entity.Course;
import com.example.course_service.entity.Lesson;
import com.example.course_service.entity.LessonType;
import com.example.course_service.entity.Option;
import com.example.course_service.entity.Question;
import com.example.course_service.entity.QuestionType;
import com.example.course_service.entity.Quiz;
import com.example.course_service.entity.Section;
import com.example.course_service.exception.BusinessRuleException;
import com.example.course_service.exception.ConflictException;
import com.example.course_service.exception.ResourceNotFoundException;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.repository.QuizRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {

	private final QuizRepository quizRepository;
	private final LessonRepository lessonRepository;

	public List<QuizSummaryResponse> findAll(Integer lessonId) {
		List<Quiz> quizzes = quizRepository.findAllWithContext();
		return quizzes.stream()
				.filter(quiz -> lessonId == null || quiz.getLesson().getId().equals(lessonId))
				.map(this::toSummary)
				.toList();
	}

	public QuizResponse findById(Integer id) {
		return toResponse(requireQuiz(id));
	}

	/** Danh sách bài học loại QUIZ kèm quiz hiện có (nếu đã có) để client chọn khi tạo mới. */
	public List<LessonOptionResponse> findLessonOptions() {
		Map<Integer, Quiz> quizByLessonId = quizRepository.findAllWithContext().stream()
				.collect(Collectors.toMap(quiz -> quiz.getLesson().getId(), Function.identity()));

		return lessonRepository.findAllWithContextByType(LessonType.QUIZ).stream()
				.map(lesson -> {
					Section section = lesson.getSection();
					Course course = section.getCourse();
					Quiz existing = quizByLessonId.get(lesson.getId());
					return new LessonOptionResponse(
							lesson.getId(), lesson.getTitle(),
							section.getId(), section.getTitle(),
							course.getId(), course.getTitle(),
							existing == null ? null : existing.getId(),
							existing == null ? null : existing.getTitle());
				})
				.toList();
	}

	@Transactional
	public QuizResponse create(QuizRequest request) {
		Lesson lesson = requireQuizLesson(request.lessonId());
		if (quizRepository.existsByLessonId(lesson.getId())) {
			throw new ConflictException("Bài học này đã có quiz. Mỗi bài học chỉ có một quiz.");
		}

		validateQuestions(request.questions());

		Quiz quiz = Quiz.builder()
				.lesson(lesson)
				.title(request.title().trim())
				.description(trimToNull(request.description()))
				.passingScore(request.passingScore())
				.build();
		applyQuestions(quiz, request.questions());

		return toResponse(quizRepository.save(quiz));
	}

	@Transactional
	public QuizResponse update(Integer id, QuizRequest request) {
		Quiz quiz = requireQuiz(id);
		Lesson lesson = requireQuizLesson(request.lessonId());

		if (!quiz.getLesson().getId().equals(lesson.getId())
				&& quizRepository.existsByLessonIdAndIdNot(lesson.getId(), id)) {
			throw new ConflictException("Bài học này đã có quiz khác. Mỗi bài học chỉ có một quiz.");
		}

		validateQuestions(request.questions());

		quiz.setLesson(lesson);
		quiz.setTitle(request.title().trim());
		quiz.setDescription(trimToNull(request.description()));
		quiz.setPassingScore(request.passingScore());
		applyQuestions(quiz, request.questions());

		return toResponse(quizRepository.save(quiz));
	}

	@Transactional
	public void delete(Integer id) {
		Quiz quiz = requireQuiz(id);
		quizRepository.delete(quiz);
	}

	private Quiz requireQuiz(Integer id) {
		return quizRepository.findByIdWithContext(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quiz id=" + id));
	}

	private Lesson requireQuizLesson(Integer lessonId) {
		Lesson lesson = lessonRepository.findById(lessonId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học id=" + lessonId));
		if (lesson.getType() != LessonType.QUIZ) {
			throw new BusinessRuleException("Chỉ bài học loại QUIZ mới có thể gắn quiz");
		}
		return lesson;
	}

	private void validateQuestions(List<QuestionRequest> questions) {
		for (int i = 0; i < questions.size(); i++) {
			QuestionRequest question = questions.get(i);
			int order = i + 1;
			long correctCount = question.options().stream().filter(OptionRequest::correct).count();

			if (correctCount == 0) {
				throw new BusinessRuleException("Câu hỏi " + order + " phải có ít nhất 1 đáp án đúng");
			}
			if (question.type() != QuestionType.MULTIPLE_CHOICE && correctCount != 1) {
				throw new BusinessRuleException(
						"Câu hỏi " + order + " loại " + question.type() + " phải có đúng 1 đáp án đúng");
			}
		}
	}

	/**
	 * Đồng bộ câu hỏi theo kiểu "upsert theo id": câu hỏi/đáp án có id thì cập nhật,
	 * không có id thì tạo mới, phần không còn trong request thì bị xoá (orphanRemoval).
	 */
	private void applyQuestions(Quiz quiz, List<QuestionRequest> requests) {
		Map<Integer, Question> staleQuestions = quiz.getQuestions().stream()
				.collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));

		int questionPosition = 1;
		for (QuestionRequest request : requests) {
			Question question;
			if (request.id() == null) {
				question = new Question();
				quiz.addQuestion(question);
			}
			else {
				question = staleQuestions.remove(request.id());
				if (question == null) {
					throw new BusinessRuleException(
							"Không tìm thấy câu hỏi id=" + request.id() + " trong quiz này");
				}
			}
			question.setContent(request.content().trim());
			question.setType(request.type());
			question.setPosition(questionPosition++);
			applyOptions(question, request.options());
		}

		new ArrayList<>(staleQuestions.values()).forEach(quiz::removeQuestion);
	}

	private void applyOptions(Question question, List<OptionRequest> requests) {
		Map<Integer, Option> staleOptions = question.getOptions().stream()
				.collect(Collectors.toMap(Option::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));

		int optionPosition = 1;
		for (OptionRequest request : requests) {
			Option option;
			if (request.id() == null) {
				option = new Option();
				question.addOption(option);
			}
			else {
				option = staleOptions.remove(request.id());
				if (option == null) {
					throw new BusinessRuleException(
							"Không tìm thấy đáp án id=" + request.id() + " trong câu hỏi này");
				}
			}
			option.setContent(request.content().trim());
			option.setCorrect(request.correct());
			option.setPosition(optionPosition++);
		}

		new ArrayList<>(staleOptions.values()).forEach(question::removeOption);
	}

	private QuizSummaryResponse toSummary(Quiz quiz) {
		Lesson lesson = quiz.getLesson();
		Course course = lesson.getSection().getCourse();
		return new QuizSummaryResponse(
				quiz.getId(), lesson.getId(), lesson.getTitle(),
				course.getId(), course.getTitle(),
				quiz.getTitle(), quiz.getPassingScore(),
				quiz.getQuestions().size(), quiz.getUpdatedAt());
	}

	private QuizResponse toResponse(Quiz quiz) {
		Lesson lesson = quiz.getLesson();
		Section section = lesson.getSection();
		Course course = section.getCourse();
		return new QuizResponse(
				quiz.getId(),
				lesson.getId(), lesson.getTitle(),
				section.getId(), section.getTitle(),
				course.getId(), course.getTitle(),
				quiz.getTitle(), quiz.getDescription(), quiz.getPassingScore(),
				quiz.getCreatedAt(), quiz.getUpdatedAt(),
				quiz.getQuestions().stream().map(this::toQuestionResponse).toList());
	}

	private QuestionResponse toQuestionResponse(Question question) {
		return new QuestionResponse(
				question.getId(), question.getContent(), question.getType(), question.getPosition(),
				question.getOptions().stream()
						.map(option -> new OptionResponse(
								option.getId(), option.getContent(), option.isCorrect(), option.getPosition()))
						.toList());
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
