package com.example.course_service.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.course_service.dto.LessonOptionResponse;
import com.example.course_service.dto.OptionRequest;
import com.example.course_service.dto.OptionResponse;
import com.example.course_service.dto.PreviewOptionResponse;
import com.example.course_service.dto.PreviewQuestionResponse;
import com.example.course_service.dto.QuestionRequest;
import com.example.course_service.dto.QuestionResponse;
import com.example.course_service.dto.QuizPreviewResponse;
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
import com.example.course_service.repository.QuestionRepository;
import com.example.course_service.repository.QuizRepository;
import com.example.course_service.service.QuizService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizServiceImpl implements QuizService {

	private static final int MIN_OPTIONS = 2;

	private final QuizRepository quizRepository;
	private final LessonRepository lessonRepository;
	private final QuestionRepository questionRepository;

	// ================================================================ truy vấn

	@Override
	public List<QuizSummaryResponse> findAll(UUID lessonId, UUID courseId, String keyword) {
		String needle = normalize(keyword);

		return quizRepository.findAllWithContext().stream()
				.filter(quiz -> lessonId == null || quiz.getLesson().getId().equals(lessonId))
				.filter(quiz -> courseId == null
						|| quiz.getLesson().getSection().getCourse().getId().equals(courseId))
				.filter(quiz -> needle == null || matches(quiz, needle))
				.map(this::toSummary)
				.toList();
	}

	@Override
	public QuizResponse findById(UUID id) {
		return toResponse(requireQuiz(id));
	}

	@Override
	public QuizPreviewResponse preview(UUID id) {
		Quiz quiz = requireQuiz(id);
		Lesson lesson = quiz.getLesson();
		Section section = lesson.getSection();
		Course course = section.getCourse();

		return new QuizPreviewResponse(
				quiz.getId(), quiz.getTitle(), quiz.getDescription(), quiz.getPassingScore(),
				lesson.getTitle(), section.getTitle(), course.getTitle(),
				sortedQuestions(quiz).stream()
						.map(question -> new PreviewQuestionResponse(
								question.getId(), question.getContent(), question.getType(), question.getPosition(),
								question.getOptions().stream()
										.map(option -> new PreviewOptionResponse(
												option.getId(), option.getContent(), option.getPosition()))
										.toList()))
						.toList());
	}

	/** Danh sách bài học loại QUIZ kèm quiz hiện có (nếu đã có) để client chọn khi tạo mới. */
	@Override
	public List<LessonOptionResponse> findLessonOptions() {
		Map<UUID, Quiz> quizByLessonId = quizRepository.findAllWithContext().stream()
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

	// ============================================================ toàn bộ quiz

	@Override
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

		return toResponse(quizRepository.saveAndFlush(quiz));
	}

	@Override
	@Transactional
	public QuizResponse update(UUID id, QuizRequest request) {
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

		return toResponse(quizRepository.saveAndFlush(quiz));
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		quizRepository.delete(requireQuiz(id));
	}

	@Override
	@Transactional
	public QuizResponse duplicate(UUID id, UUID targetLessonId) {
		Quiz source = requireQuiz(id);
		Lesson targetLesson = requireQuizLesson(targetLessonId);
		if (quizRepository.existsByLessonId(targetLesson.getId())) {
			throw new ConflictException("Bài học đích đã có quiz. Mỗi bài học chỉ có một quiz.");
		}

		Quiz copy = Quiz.builder()
				.lesson(targetLesson)
				.title(source.getTitle())
				.description(source.getDescription())
				.passingScore(source.getPassingScore())
				.build();

		int questionPosition = 1;
		for (Question sourceQuestion : source.getQuestions()) {
			Question question = Question.builder()
					.content(sourceQuestion.getContent())
					.type(sourceQuestion.getType())
					.position(questionPosition++)
					.build();
			int optionPosition = 1;
			for (Option sourceOption : sourceQuestion.getOptions()) {
				question.addOption(Option.builder()
						.content(sourceOption.getContent())
						.isCorrect(sourceOption.isCorrect())
						.position(optionPosition++)
						.build());
			}
			copy.addQuestion(question);
		}

		return toResponse(quizRepository.saveAndFlush(copy));
	}

	// ================================================================ câu hỏi

	@Override
	@Transactional
	public QuestionResponse addQuestion(UUID quizId, QuestionRequest request) {
		Quiz quiz = requireQuiz(quizId);
		validateQuestion(request, quiz.getQuestions().size() + 1);

		Question question = Question.builder()
				.content(request.content().trim())
				.type(request.type())
				.position(nextQuestionPosition(quiz))
				.build();
		applyOptions(question, request.options());
		quiz.addQuestion(question);

		return toQuestionResponse(questionRepository.saveAndFlush(question));
	}

	@Override
	@Transactional
	public QuestionResponse updateQuestion(UUID quizId, UUID questionId, QuestionRequest request) {
		Quiz quiz = requireQuiz(quizId);
		Question question = requireQuestion(quiz, questionId);
		validateQuestion(request, question.getPosition() == null ? 1 : question.getPosition());

		question.setContent(request.content().trim());
		question.setType(request.type());
		applyOptions(question, request.options());

		return toQuestionResponse(questionRepository.saveAndFlush(question));
	}

	@Override
	@Transactional
	public void deleteQuestion(UUID quizId, UUID questionId) {
		Quiz quiz = requireQuiz(quizId);
		Question question = requireQuestion(quiz, questionId);
		if (quiz.getQuestions().size() <= 1) {
			throw new BusinessRuleException("Quiz phải có ít nhất 1 câu hỏi. Hãy thêm câu hỏi khác trước khi xoá.");
		}

		quiz.removeQuestion(question);
		resequenceQuestions(quiz);
		quizRepository.flush();
	}

	@Override
	@Transactional
	public List<QuestionResponse> reorderQuestions(UUID quizId, List<UUID> orderedIds) {
		Quiz quiz = requireQuiz(quizId);
		List<Question> questions = new ArrayList<>(quiz.getQuestions());

		Set<UUID> expected = questions.stream().map(Question::getId).collect(Collectors.toSet());
		Set<UUID> actual = new HashSet<>(orderedIds);
		if (orderedIds.size() != questions.size() || !expected.equals(actual)) {
			throw new BusinessRuleException(
					"Danh sách thứ tự phải chứa đúng toàn bộ id câu hỏi hiện có của quiz");
		}

		Map<UUID, Question> byId = questions.stream()
				.collect(Collectors.toMap(Question::getId, Function.identity()));
		for (int i = 0; i < orderedIds.size(); i++) {
			byId.get(orderedIds.get(i)).setPosition(i + 1);
		}

		quizRepository.flush();
		return sortedQuestions(quiz).stream()
				.map(this::toQuestionResponse)
				.toList();
	}

	// ================================================================ nội bộ

	private Quiz requireQuiz(UUID id) {
		return quizRepository.findByIdWithContext(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quiz id=" + id));
	}

	private Question requireQuestion(Quiz quiz, UUID questionId) {
		return quiz.getQuestions().stream()
				.filter(question -> question.getId().equals(questionId))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException(
						"Không tìm thấy câu hỏi id=" + questionId + " trong quiz id=" + quiz.getId()));
	}

	private Lesson requireQuizLesson(UUID lessonId) {
		Lesson lesson = lessonRepository.findById(lessonId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học id=" + lessonId));
		if (lesson.getType() != LessonType.QUIZ) {
			throw new BusinessRuleException("Chỉ bài học loại QUIZ mới có thể gắn quiz");
		}
		return lesson;
	}

	private int nextQuestionPosition(Quiz quiz) {
		return quiz.getQuestions().stream()
				.map(Question::getPosition)
				.filter(Objects::nonNull)
				.max(Integer::compareTo)
				.orElse(0) + 1;
	}

	private List<Question> sortedQuestions(Quiz quiz) {
		return quiz.getQuestions().stream()
				.sorted((a, b) -> {
					int left = a.getPosition() == null ? 0 : a.getPosition();
					int right = b.getPosition() == null ? 0 : b.getPosition();
					return Integer.compare(left, right);
				})
				.toList();
	}

	private void resequenceQuestions(Quiz quiz) {
		List<Question> ordered = quiz.getQuestions().stream()
				.sorted((a, b) -> a.getPosition().compareTo(b.getPosition()))
				.toList();
		for (int i = 0; i < ordered.size(); i++) {
			ordered.get(i).setPosition(i + 1);
		}
	}

	private void validateQuestions(List<QuestionRequest> questions) {
		for (int i = 0; i < questions.size(); i++) {
			validateQuestion(questions.get(i), i + 1);
		}
	}

	private void validateQuestion(QuestionRequest question, int order) {
		long correctCount = question.options().stream().filter(OptionRequest::correct).count();

		if (question.options().size() < MIN_OPTIONS) {
			throw new BusinessRuleException("Câu hỏi " + order + " phải có ít nhất " + MIN_OPTIONS + " đáp án");
		}
		if (question.type() == QuestionType.TRUE_FALSE && question.options().size() != 2) {
			throw new BusinessRuleException("Câu hỏi " + order + " loại TRUE_FALSE phải có đúng 2 đáp án");
		}
		if (correctCount == 0) {
			throw new BusinessRuleException("Câu hỏi " + order + " phải có ít nhất 1 đáp án đúng");
		}
		if (question.type() != QuestionType.MULTIPLE_CHOICE && correctCount != 1) {
			throw new BusinessRuleException(
					"Câu hỏi " + order + " loại " + question.type() + " phải có đúng 1 đáp án đúng");
		}

		Set<String> seen = new HashSet<>();
		for (OptionRequest option : question.options()) {
			String normalized = normalize(option.content());
			if (normalized != null && !seen.add(normalized)) {
				throw new BusinessRuleException("Câu hỏi " + order + " có đáp án bị trùng nội dung");
			}
		}
	}

	/**
	 * Đồng bộ câu hỏi theo kiểu "upsert theo id": câu hỏi/đáp án có id thì cập nhật,
	 * không có id thì tạo mới, phần không còn trong request thì bị xoá (orphanRemoval).
	 */
	private void applyQuestions(Quiz quiz, List<QuestionRequest> requests) {
		Map<UUID, Question> staleQuestions = quiz.getQuestions().stream()
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
		Map<UUID, Option> staleOptions = question.getOptions().stream()
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

	// ============================================================== ánh xạ DTO

	private boolean matches(Quiz quiz, String needle) {
		Lesson lesson = quiz.getLesson();
		return contains(quiz.getTitle(), needle)
				|| contains(lesson.getTitle(), needle)
				|| contains(lesson.getSection().getCourse().getTitle(), needle);
	}

	private QuizSummaryResponse toSummary(Quiz quiz) {
		Lesson lesson = quiz.getLesson();
		Section section = lesson.getSection();
		Course course = section.getCourse();
		return new QuizSummaryResponse(
				quiz.getId(), lesson.getId(), lesson.getTitle(),
				section.getId(), section.getTitle(),
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
				sortedQuestions(quiz).stream().map(this::toQuestionResponse).toList());
	}

	private QuestionResponse toQuestionResponse(Question question) {
		return new QuestionResponse(
				question.getId(), question.getContent(), question.getType(), question.getPosition(),
				question.getOptions().stream()
						.map(option -> new OptionResponse(
								option.getId(), option.getContent(), option.isCorrect(), option.getPosition()))
						.toList());
	}

	private boolean contains(String value, String needle) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
	}

	private String normalize(String value) {
		String trimmed = trimToNull(value);
		return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
