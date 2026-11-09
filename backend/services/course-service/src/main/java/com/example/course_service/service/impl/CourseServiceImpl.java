package com.example.course_service.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.example.course_service.dto.CourseDetailResponse;
import com.example.course_service.dto.CourseRequest;
import com.example.course_service.dto.CourseSummaryResponse;
import com.example.course_service.dto.LessonRequest;
import com.example.course_service.dto.LessonResponse;
import com.example.course_service.dto.MaterialRequest;
import com.example.course_service.dto.MaterialResponse;
import com.example.course_service.dto.PublishedCurriculumResponse;
import com.example.course_service.dto.SectionRequest;
import com.example.course_service.dto.SectionResponse;
import com.example.course_service.entity.Course;
import com.example.course_service.entity.CourseStatus;
import com.example.course_service.entity.Lesson;
import com.example.course_service.entity.LessonType;
import com.example.course_service.entity.Material;
import com.example.course_service.entity.MaterialType;
import com.example.course_service.entity.Quiz;
import com.example.course_service.entity.Section;
import com.example.course_service.exception.BusinessRuleException;
import com.example.course_service.exception.ConflictException;
import com.example.course_service.exception.ResourceNotFoundException;
import com.example.course_service.repository.CourseCount;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.repository.MaterialRepository;
import com.example.course_service.repository.QuizRepository;
import com.example.course_service.repository.SectionRepository;
import com.example.course_service.security.CourseAccessGuard;
import com.example.course_service.service.CourseService;
import com.example.course_service.service.StorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

	/**
	 * Vòng đời khoá học. Giảng viên tự xuất bản, không có bước duyệt.
	 * DRAFT → PUBLISHED ⇄ UNPUBLISHED; mọi trạng thái có thể lưu trữ (ARCHIVED), ARCHIVED khôi phục về DRAFT.
	 */
	private static final Map<CourseStatus, List<CourseStatus>> TRANSITIONS = new EnumMap<>(CourseStatus.class);

	static {
		TRANSITIONS.put(CourseStatus.DRAFT, List.of(CourseStatus.PUBLISHED, CourseStatus.ARCHIVED));
		TRANSITIONS.put(CourseStatus.PUBLISHED, List.of(CourseStatus.UNPUBLISHED, CourseStatus.ARCHIVED));
		TRANSITIONS.put(CourseStatus.UNPUBLISHED,
				List.of(CourseStatus.PUBLISHED, CourseStatus.DRAFT, CourseStatus.ARCHIVED));
		TRANSITIONS.put(CourseStatus.ARCHIVED, List.of(CourseStatus.DRAFT));
	}

	private final CourseRepository courseRepository;
	private final SectionRepository sectionRepository;
	private final LessonRepository lessonRepository;
	private final MaterialRepository materialRepository;
	private final QuizRepository quizRepository;
	private final StorageService storageService;
	private final CourseAccessGuard courseAccessGuard;
	private final PlatformTransactionManager transactionManager;

	private static final Logger log = LoggerFactory.getLogger(CourseServiceImpl.class);

	/** Học liệu upload lên MinIO lưu cột url dạng {@code minio:<object key>}; link xem được sinh khi đọc. */
	private static final String STORED_PREFIX = "minio:";

	private static final long MAX_THUMBNAIL_BYTES = 5L * 1024 * 1024;

	// ================================================================ khoá học

	@Override
	public List<CourseSummaryResponse> search(String keyword, CourseStatus status, UUID instructorId) {
		String pattern = keyword == null || keyword.isBlank()
				? null
				: "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";

		Map<UUID, Long> sectionCounts = toMap(sectionRepository.countByCourse());
		Map<UUID, Long> lessonCounts = toMap(lessonRepository.countByCourse());

		return courseRepository.search(pattern, status, instructorId).stream()
				.filter(course -> course.getStatus() != CourseStatus.DRAFT || courseAccessGuard.canSeeDraft(course))
				.map(course -> new CourseSummaryResponse(
						course.getId(), course.getInstructorId(), course.getTitle(), course.getDescription(),
						viewUrl(course.getThumbnailUrl()), course.getStatus(),
						sectionCounts.getOrDefault(course.getId(), 0L),
						lessonCounts.getOrDefault(course.getId(), 0L),
						course.getCreatedAt(), course.getUpdatedAt()))
				.toList();
	}

	@Override
	public CourseDetailResponse getDetail(UUID id) {
		Course course = requireCourse(id);
		// Khoá học nháp không hiện với học viên: coi như không tồn tại để không lộ nội dung đang soạn.
		if (course.getStatus() == CourseStatus.DRAFT && !courseAccessGuard.canSeeDraft(course)) {
			throw new ResourceNotFoundException("Không tìm thấy khoá học id=" + id);
		}
		return toDetail(course);
	}

	@Override
	public PublishedCurriculumResponse getPublishedCurriculum(UUID id) {
		Course course = requireCourse(id);
		if (course.getStatus() != CourseStatus.PUBLISHED) {
			throw new ResourceNotFoundException("Khoá học id=" + id + " chưa được phát hành");
		}

		List<Lesson> allLessons = lessonRepository.findByCourseIdOrdered(id);
		Map<UUID, UUID> quizIdByLesson = quizIdsByLesson(allLessons);

		// Giảng viên vẫn sửa được nội dung khi khoá đang xuất bản, nên có thể có bài Quiz chưa tạo quiz
		// hoặc chương chưa có bài. Chỉ đưa ra những gì học viên học được: bỏ bài Quiz chưa có quiz và
		// chương rỗng, để Progress Service không phải xử lý bài không thể hoàn thành.
		List<Lesson> readyLessons = allLessons.stream()
				.filter(lesson -> lesson.getType() != LessonType.QUIZ || quizIdByLesson.containsKey(lesson.getId()))
				.toList();
		Map<UUID, List<Lesson>> lessonsBySection = readyLessons.stream()
				.collect(Collectors.groupingBy(lesson -> lesson.getSection().getId()));

		List<PublishedCurriculumResponse.Section> sections = sectionRepository
				.findByCourseIdOrderByPositionAscIdAsc(id).stream()
				.filter(section -> lessonsBySection.containsKey(section.getId()))
				.map(section -> new PublishedCurriculumResponse.Section(
						section.getId(), section.getTitle(), section.getPosition(),
						lessonsBySection.get(section.getId()).stream()
								.map(lesson -> new PublishedCurriculumResponse.Lesson(
										lesson.getId(), lesson.getTitle(), lesson.getType(),
										lesson.getPosition(), lesson.isPreview(),
										quizIdByLesson.get(lesson.getId())))
								.toList()))
				.toList();

		return new PublishedCurriculumResponse(course.getId(), course.getTitle(), readyLessons.size(), sections);
	}

	@Override
	@Transactional
	public CourseDetailResponse create(CourseRequest request) {
		courseAccessGuard.checkCanAssignInstructor(request.instructorId());
		Course course = Course.builder()
				.instructorId(request.instructorId())
				.title(request.title().trim())
				.description(trimToNull(request.description()))
				.thumbnailUrl(trimToNull(request.thumbnailUrl()))
				.status(CourseStatus.DRAFT)
				.build();
		return toDetail(courseRepository.saveAndFlush(course));
	}

	@Override
	@Transactional
	public CourseDetailResponse update(UUID id, CourseRequest request) {
		Course course = requireManageableCourse(id);
		ensureEditable(course);
		courseAccessGuard.checkCanAssignInstructor(request.instructorId());
		course.setInstructorId(request.instructorId());
		course.setTitle(request.title().trim());
		course.setDescription(trimToNull(request.description()));

		// Để trống thumbnailUrl = giữ nguyên ảnh bìa hiện tại (có thể là ảnh đã upload lên MinIO).
		// Muốn bỏ ảnh bìa dùng DELETE /courses/{id}/thumbnail.
		String newThumbnail = trimToNull(request.thumbnailUrl());
		if (newThumbnail != null) {
			replaceThumbnail(course, newThumbnail);
		}
		return toDetail(courseRepository.saveAndFlush(course));
	}

	/** Giống {@link #uploadMaterial}: upload ngoài transaction, lưu DB trong transaction ngắn. */
	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public CourseDetailResponse uploadThumbnail(UUID id, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessRuleException("File ảnh bìa đang trống");
		}
		if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
			throw new BusinessRuleException("Ảnh bìa phải là file hình ảnh (JPG, PNG, WEBP…)");
		}
		if (file.getSize() > MAX_THUMBNAIL_BYTES) {
			throw new BusinessRuleException("Ảnh bìa tối đa 5 MB");
		}
		readTransaction().executeWithoutResult(status -> ensureEditable(requireManageableCourse(id)));

		StorageService.StoredFile stored = storageService.upload("courses/" + id + "/thumbnail", file);

		return saveOrDiscardUpload(stored, () -> {
			Course course = requireCourse(id);
			ensureEditable(course);
			replaceThumbnail(course, STORED_PREFIX + stored.objectKey());
			return toDetail(courseRepository.saveAndFlush(course));
		});
	}

	/**
	 * Lưu DB cho file vừa upload trong một transaction ghi ngắn. Lỗi ở bất kỳ bước nào, kể cả khi commit,
	 * thì xoá file trên MinIO rồi ném lại lỗi.
	 */
	private CourseDetailResponse saveOrDiscardUpload(StorageService.StoredFile stored,
			java.util.function.Supplier<CourseDetailResponse> save) {
		try {
			return new TransactionTemplate(transactionManager).execute(status -> save.get());
		}
		catch (RuntimeException ex) {
			deleteStoredFilesQuietly(List.of(stored.objectKey()));
			throw ex;
		}
	}

	private TransactionTemplate readTransaction() {
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template;
	}

	@Override
	@Transactional
	public CourseDetailResponse removeThumbnail(UUID id) {
		Course course = requireManageableCourse(id);
		ensureEditable(course);
		replaceThumbnail(course, null);
		return toDetail(courseRepository.saveAndFlush(course));
	}

	/** Đổi ảnh bìa; nếu ảnh cũ nằm trên MinIO thì xoá file cũ sau khi commit. */
	private void replaceThumbnail(Course course, String newValue) {
		String oldKey = storedKey(course.getThumbnailUrl());
		course.setThumbnailUrl(newValue);
		if (oldKey != null && !(STORED_PREFIX + oldKey).equals(newValue)) {
			deleteStoredFilesAfterCommit(List.of(oldKey));
		}
	}

	@Override
	@Transactional
	public CourseDetailResponse changeStatus(UUID id, CourseStatus target) {
		Course course = requireManageableCourse(id);
		CourseStatus current = course.getStatus();

		if (current == target) {
			throw new BusinessRuleException("Khoá học đã ở trạng thái " + target);
		}
		if (!allowedTransitions(current).contains(target)) {
			throw new ConflictException("Không thể chuyển khoá học từ " + current + " sang " + target);
		}
		if (target == CourseStatus.PUBLISHED) {
			validateCurriculumComplete(course);
		}

		course.setStatus(target);
		return toDetail(courseRepository.saveAndFlush(course));
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		Course course = requireManageableCourse(id);
		// Chỉ xoá hẳn khoá học chưa từng xuất bản (bản nháp) hoặc đã lưu trữ.
		if (course.getStatus() != CourseStatus.DRAFT && course.getStatus() != CourseStatus.ARCHIVED) {
			throw new ConflictException("Khoá học đang " + course.getStatus()
					+ " nên không thể xoá. Hãy ẩn (UNPUBLISHED) rồi lưu trữ (ARCHIVED) trước.");
		}
		deleteLessons(lessonRepository.findByCourseIdOrdered(id));
		sectionRepository.deleteAll(sectionRepository.findByCourseIdOrderByPositionAscIdAsc(id));
		sectionRepository.flush();
		replaceThumbnail(course, null);
		courseRepository.delete(course);
	}

	// ================================================================== chương

	@Override
	@Transactional
	public CourseDetailResponse addSection(UUID courseId, SectionRequest request) {
		Course course = requireManageableCourse(courseId);
		ensureEditable(course);

		sectionRepository.save(Section.builder()
				.course(course)
				.title(request.title().trim())
				.description(trimToNull(request.description()))
				.position(sectionRepository.findMaxPosition(courseId) + 1)
				.build());
		return refreshed(courseId);
	}

	@Override
	@Transactional
	public CourseDetailResponse updateSection(UUID sectionId, SectionRequest request) {
		Section section = requireEditableSection(sectionId);
		section.setTitle(request.title().trim());
		section.setDescription(trimToNull(request.description()));
		return refreshed(section.getCourse().getId());
	}

	@Override
	@Transactional
	public CourseDetailResponse deleteSection(UUID sectionId) {
		Section section = requireEditableSection(sectionId);
		UUID courseId = section.getCourse().getId();

		deleteLessons(lessonRepository.findBySectionIdOrderByPositionAscIdAsc(sectionId));
		sectionRepository.delete(section);
		sectionRepository.flush();

		renumber(sectionRepository.findByCourseIdOrderByPositionAscIdAsc(courseId), Section::setPosition);
		return refreshed(courseId);
	}

	@Override
	@Transactional
	public CourseDetailResponse reorderSections(UUID courseId, List<UUID> orderedIds) {
		Course course = requireManageableCourse(courseId);
		ensureEditable(course);

		List<Section> sections = sectionRepository.findByCourseIdOrderByPositionAscIdAsc(courseId);
		applyOrder(sections, Section::getId, Section::setPosition, orderedIds, "chương");
		return refreshed(courseId);
	}

	// ================================================================= bài học

	@Override
	@Transactional
	public CourseDetailResponse addLesson(UUID sectionId, LessonRequest request) {
		Section section = requireEditableSection(sectionId);

		lessonRepository.save(Lesson.builder()
				.section(section)
				.title(request.title().trim())
				.description(trimToNull(request.description()))
				.type(request.type())
				.isPreview(Boolean.TRUE.equals(request.preview()))
				.position(lessonRepository.findMaxPosition(sectionId) + 1)
				.build());
		return refreshed(section.getCourse().getId());
	}

	@Override
	@Transactional
	public CourseDetailResponse updateLesson(UUID lessonId, LessonRequest request) {
		Lesson lesson = requireEditableLesson(lessonId);

		if (lesson.getType() == LessonType.QUIZ && request.type() != LessonType.QUIZ
				&& quizRepository.existsByLessonId(lessonId)) {
			throw new ConflictException("Bài học đang có quiz. Hãy xoá quiz trước khi đổi loại bài học.");
		}

		lesson.setTitle(request.title().trim());
		lesson.setDescription(trimToNull(request.description()));
		lesson.setType(request.type());
		lesson.setPreview(Boolean.TRUE.equals(request.preview()));
		return refreshed(lesson.getSection().getCourse().getId());
	}

	@Override
	@Transactional
	public CourseDetailResponse deleteLesson(UUID lessonId) {
		Lesson lesson = requireEditableLesson(lessonId);
		UUID sectionId = lesson.getSection().getId();
		UUID courseId = lesson.getSection().getCourse().getId();

		deleteLessons(List.of(lesson));
		renumber(lessonRepository.findBySectionIdOrderByPositionAscIdAsc(sectionId), Lesson::setPosition);
		return refreshed(courseId);
	}

	@Override
	@Transactional
	public CourseDetailResponse reorderLessons(UUID sectionId, List<UUID> orderedIds) {
		Section section = requireEditableSection(sectionId);
		List<Lesson> lessons = lessonRepository.findBySectionIdOrderByPositionAscIdAsc(sectionId);
		applyOrder(lessons, Lesson::getId, Lesson::setPosition, orderedIds, "bài học");
		return refreshed(section.getCourse().getId());
	}

	// ================================================================ học liệu

	@Override
	@Transactional
	public CourseDetailResponse addMaterial(UUID lessonId, MaterialRequest request) {
		Lesson lesson = requireEditableLesson(lessonId);
		if (trimToNull(request.url()) == null) {
			throw new BusinessRuleException("URL học liệu không được để trống. Muốn đưa file lên hãy dùng Upload.");
		}

		Material material = Material.builder()
				.lesson(lesson)
				.position(materialRepository.findMaxPosition(lessonId) + 1)
				.build();
		applyMaterial(material, request);
		materialRepository.save(material);
		return refreshed(lesson.getSection().getCourse().getId());
	}

	/**
	 * Upload chạy NGOÀI transaction: video có thể mất vài phút, không được giữ kết nối DB suốt thời gian đó.
	 * Quy trình: kiểm tra (transaction đọc ngắn) → đẩy file lên MinIO → lưu DB (transaction ghi ngắn).
	 * Nếu bước lưu DB lỗi, kể cả lúc commit, file vừa upload bị xoá để không bỏ lại file mồ côi.
	 */
	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public CourseDetailResponse uploadMaterial(UUID lessonId, MultipartFile file, String name, MaterialType type) {
		if (file == null || file.isEmpty()) {
			throw new BusinessRuleException("File upload đang trống");
		}
		UUID courseId = readTransaction().execute(
				status -> requireEditableLesson(lessonId).getSection().getCourse().getId());

		StorageService.StoredFile stored = storageService.upload(
				"courses/" + courseId + "/lessons/" + lessonId, file);
		String displayName = trimToNull(name) != null ? name.trim() : trimToNull(file.getOriginalFilename());

		return saveOrDiscardUpload(stored, () -> {
			// Kiểm tra lại: bài học có thể vừa bị xoá / khoá học vừa bị lưu trữ trong lúc upload.
			Lesson lesson = requireEditableLesson(lessonId);
			materialRepository.saveAndFlush(Material.builder()
					.lesson(lesson)
					.name(truncate(displayName == null ? "Học liệu" : displayName, 255))
					.type(type != null ? type : guessType(stored.contentType()))
					.url(STORED_PREFIX + stored.objectKey())
					.fileSize(stored.size())
					.mimeType(truncate(stored.contentType(), 100))
					.position(materialRepository.findMaxPosition(lessonId) + 1)
					.build());
			return refreshed(lesson.getSection().getCourse().getId());
		});
	}

	@Override
	@Transactional
	public CourseDetailResponse updateMaterial(UUID materialId, MaterialRequest request) {
		Material material = requireEditableMaterial(materialId);
		String oldStoredKey = storedKey(material);
		String newUrl = trimToNull(request.url());

		if (newUrl == null && oldStoredKey == null) {
			throw new BusinessRuleException("URL học liệu không được để trống");
		}
		if (newUrl == null) {
			// File trên MinIO: chỉ sửa tên / loại, giữ nguyên file, dung lượng và MIME type.
			material.setName(request.name().trim());
			material.setType(request.type());
		}
		else {
			applyMaterial(material, request);
			if (oldStoredKey != null) {
				deleteStoredFilesAfterCommit(List.of(oldStoredKey));
			}
		}
		return refreshed(material.getLesson().getSection().getCourse().getId());
	}

	@Override
	@Transactional
	public CourseDetailResponse deleteMaterial(UUID materialId) {
		Material material = requireEditableMaterial(materialId);
		Lesson lesson = material.getLesson();
		UUID courseId = lesson.getSection().getCourse().getId();

		String storedKey = storedKey(material);
		materialRepository.delete(material);
		materialRepository.flush();
		if (storedKey != null) {
			deleteStoredFilesAfterCommit(List.of(storedKey));
		}
		List<Material> remaining = materialRepository.findByLessonIdIn(List.of(lesson.getId())).stream()
				.sorted((a, b) -> Integer.compare(a.getPosition(), b.getPosition()))
				.toList();
		renumber(remaining, Material::setPosition);
		return refreshed(courseId);
	}

	// ========================================================= quy tắc trạng thái

	private static List<CourseStatus> allowedTransitions(CourseStatus from) {
		return TRANSITIONS.getOrDefault(from, List.of());
	}

	/** Khoá học đã lưu trữ thì chỉ đọc; muốn sửa phải khôi phục về DRAFT. */
	private static boolean isEditable(CourseStatus status) {
		return status != CourseStatus.ARCHIVED;
	}

	private static void ensureEditable(Course course) {
		if (!isEditable(course.getStatus())) {
			throw new BusinessRuleException("Khoá học đã lưu trữ, hãy khôi phục về DRAFT trước khi chỉnh sửa");
		}
	}

	/** Xuất bản yêu cầu: có ít nhất 1 chương, mọi chương có bài học, mọi bài QUIZ đã có quiz. */
	private void validateCurriculumComplete(Course course) {
		List<Section> sections = sectionRepository.findByCourseIdOrderByPositionAscIdAsc(course.getId());
		if (sections.isEmpty()) {
			throw new BusinessRuleException("Khoá học phải có ít nhất 1 chương trước khi xuất bản");
		}
		List<Lesson> lessons = lessonRepository.findByCourseIdOrdered(course.getId());
		Map<UUID, Long> lessonsPerSection = lessons.stream()
				.collect(Collectors.groupingBy(lesson -> lesson.getSection().getId(), Collectors.counting()));
		for (Section section : sections) {
			if (lessonsPerSection.getOrDefault(section.getId(), 0L) == 0) {
				throw new BusinessRuleException("Chương \"" + section.getTitle() + "\" chưa có bài học nào");
			}
		}
		Map<UUID, UUID> quizIdByLesson = quizIdsByLesson(lessons);
		for (Lesson lesson : lessons) {
			if (lesson.getType() == LessonType.QUIZ && !quizIdByLesson.containsKey(lesson.getId())) {
				throw new BusinessRuleException("Bài học \"" + lesson.getTitle() + "\" loại QUIZ chưa được tạo quiz");
			}
		}
	}

	// ============================================================ tìm & kiểm tra

	/** Như {@link #requireCourse} nhưng chặn nếu người gọi không phải giảng viên phụ trách (Admin bỏ qua). */
	private Course requireManageableCourse(UUID id) {
		Course course = requireCourse(id);
		courseAccessGuard.checkCanManage(course);
		return course;
	}

	private Course requireCourse(UUID id) {
		return courseRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khoá học id=" + id));
	}

	private Section requireEditableSection(UUID sectionId) {
		Section section = sectionRepository.findByIdWithCourse(sectionId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương id=" + sectionId));
		courseAccessGuard.checkCanManage(section.getCourse());
		ensureEditable(section.getCourse());
		return section;
	}

	private Lesson requireEditableLesson(UUID lessonId) {
		Lesson lesson = lessonRepository.findByIdWithContext(lessonId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học id=" + lessonId));
		courseAccessGuard.checkCanManage(lesson.getSection().getCourse());
		ensureEditable(lesson.getSection().getCourse());
		return lesson;
	}

	private Material requireEditableMaterial(UUID materialId) {
		Material material = materialRepository.findByIdWithContext(materialId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học liệu id=" + materialId));
		courseAccessGuard.checkCanManage(material.getLesson().getSection().getCourse());
		ensureEditable(material.getLesson().getSection().getCourse());
		return material;
	}

	// =================================================================== xoá

	/**
	 * Xoá bài học cùng dữ liệu phụ thuộc (học liệu + file trên MinIO, quiz → câu hỏi → đáp án).
	 * Entity không khai báo cascade từ Lesson xuống nên phải xoá thủ công theo đúng thứ tự khoá ngoại.
	 */
	private void deleteLessons(Collection<Lesson> lessons) {
		if (lessons.isEmpty()) {
			return;
		}
		List<UUID> lessonIds = lessons.stream().map(Lesson::getId).toList();
		List<Material> materials = materialRepository.findByLessonIdIn(lessonIds);
		List<String> storedKeys = materials.stream().map(CourseServiceImpl::storedKey)
				.filter(java.util.Objects::nonNull).toList();

		materialRepository.deleteAll(materials);
		quizRepository.deleteAll(quizRepository.findByLessonIdIn(lessonIds));
		lessonRepository.deleteAll(lessons);
		lessonRepository.flush();
		deleteStoredFilesAfterCommit(storedKeys);
	}

	// ============================================================ file MinIO

	/** Khoá object trên MinIO nếu học liệu là file đã upload, ngược lại null (học liệu dạng link). */
	private static String storedKey(Material material) {
		return storedKey(material.getUrl());
	}

	/** Khoá object nếu giá trị lưu trong DB có dạng {@code minio:<key>}, ngược lại null (link ngoài / trống). */
	private static String storedKey(String storedValue) {
		return storedValue != null && storedValue.startsWith(STORED_PREFIX)
				? storedValue.substring(STORED_PREFIX.length())
				: null;
	}

	/** Chỉ xoá file trên MinIO khi DB đã commit, tránh mất file khi transaction bị rollback. */
	private void deleteStoredFilesAfterCommit(List<String> objectKeys) {
		if (objectKeys.isEmpty()) {
			return;
		}
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			deleteStoredFilesQuietly(objectKeys);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				deleteStoredFilesQuietly(objectKeys);
			}
		});
	}

	/** Lỗi xoá file chỉ ghi log: dữ liệu DB đã đúng, file thừa trên MinIO không ảnh hưởng người dùng. */
	private void deleteStoredFilesQuietly(List<String> objectKeys) {
		for (String key : objectKeys) {
			try {
				storageService.delete(key);
			}
			catch (RuntimeException ex) {
				log.warn("Không xoá được file {} trên MinIO: {}", key, ex.getMessage());
			}
		}
	}

	private String viewUrl(Material material) {
		return viewUrl(material.getUrl());
	}

	/**
	 * Đổi giá trị lưu trong DB thành link trình duyệt mở được: file trên MinIO → presigned URL,
	 * link ngoài giữ nguyên. Không tạo được link thì trả null để vẫn hiển thị được khoá học.
	 */
	private String viewUrl(String storedValue) {
		String key = storedKey(storedValue);
		if (key == null) {
			return storedValue;
		}
		try {
			return storageService.viewUrl(key);
		}
		catch (RuntimeException ex) {
			log.warn("Không tạo được link xem file {}: {}", key, ex.getMessage());
			return null;
		}
	}

	private static MaterialType guessType(String contentType) {
		if (contentType == null) {
			return MaterialType.OTHER;
		}
		if (contentType.startsWith("video/")) {
			return MaterialType.VIDEO;
		}
		if (contentType.startsWith("image/")) {
			return MaterialType.IMAGE;
		}
		if (contentType.startsWith("text/") || contentType.equals("application/pdf")
				|| contentType.contains("word") || contentType.contains("presentation")
				|| contentType.contains("spreadsheet") || contentType.contains("excel")
				|| contentType.contains("powerpoint")) {
			return MaterialType.DOCUMENT;
		}
		return MaterialType.OTHER;
	}

	private static String truncate(String value, int max) {
		return value != null && value.length() > max ? value.substring(0, max) : value;
	}

	// =============================================================== thứ tự

	/** Danh sách id mới phải chứa đúng và đủ các phần tử hiện có, không trùng lặp. */
	private <T> void applyOrder(List<T> items, Function<T, UUID> idOf, PositionSetter<T> setter,
			List<UUID> orderedIds, String label) {
		Set<UUID> requested = new HashSet<>(orderedIds);
		Set<UUID> existing = items.stream().map(idOf).collect(Collectors.toSet());
		if (requested.size() != orderedIds.size()) {
			throw new BusinessRuleException("Danh sách " + label + " có id bị lặp");
		}
		if (!requested.equals(existing)) {
			throw new BusinessRuleException(
					"Danh sách " + label + " phải gồm đúng và đủ các id hiện có: " + existing);
		}
		Map<UUID, T> byId = items.stream().collect(Collectors.toMap(idOf, Function.identity()));
		for (int i = 0; i < orderedIds.size(); i++) {
			setter.set(byId.get(orderedIds.get(i)), i + 1);
		}
	}

	private <T> void renumber(List<T> items, PositionSetter<T> setter) {
		for (int i = 0; i < items.size(); i++) {
			setter.set(items.get(i), i + 1);
		}
	}

	@FunctionalInterface
	private interface PositionSetter<T> {
		void set(T item, int position);
	}

	// =============================================================== mapping

	/** Đẩy thay đổi xuống DB rồi đọc lại chi tiết khoá học sau thao tác ghi. */
	private CourseDetailResponse refreshed(UUID courseId) {
		sectionRepository.flush();
		return getDetail(courseId);
	}

	private CourseDetailResponse toDetail(Course course) {
		UUID courseId = course.getId();
		List<Section> sections = sectionRepository.findByCourseIdOrderByPositionAscIdAsc(courseId);
		List<Lesson> lessons = lessonRepository.findByCourseIdOrdered(courseId);
		Map<UUID, UUID> quizIdByLesson = quizIdsByLesson(lessons);

		Map<UUID, List<MaterialResponse>> materialsByLesson = materialRepository.findByCourseIdOrdered(courseId)
				.stream()
				.collect(Collectors.groupingBy(material -> material.getLesson().getId(),
						Collectors.mapping(this::toMaterialResponse, Collectors.toList())));

		Map<UUID, List<LessonResponse>> lessonsBySection = new HashMap<>();
		for (Lesson lesson : lessons) {
			lessonsBySection.computeIfAbsent(lesson.getSection().getId(), key -> new ArrayList<>())
					.add(new LessonResponse(
							lesson.getId(), lesson.getSection().getId(), lesson.getTitle(), lesson.getDescription(),
							lesson.getType(), lesson.getPosition(), lesson.isPreview(),
							quizIdByLesson.get(lesson.getId()),
							materialsByLesson.getOrDefault(lesson.getId(), List.of())));
		}

		List<SectionResponse> sectionResponses = sections.stream()
				.map(section -> new SectionResponse(
						section.getId(), courseId, section.getTitle(), section.getDescription(),
						section.getPosition(), lessonsBySection.getOrDefault(section.getId(), List.of())))
				.toList();

		return new CourseDetailResponse(
				courseId, course.getInstructorId(), course.getTitle(), course.getDescription(),
				viewUrl(course.getThumbnailUrl()), course.getStatus(),
				allowedTransitions(course.getStatus()),
				isEditable(course.getStatus()),
				course.getCreatedAt(), course.getUpdatedAt(), sectionResponses);
	}

	private Map<UUID, UUID> quizIdsByLesson(List<Lesson> lessons) {
		if (lessons.isEmpty()) {
			return Map.of();
		}
		return quizRepository.findByLessonIdIn(lessons.stream().map(Lesson::getId).toList()).stream()
				.collect(Collectors.toMap(quiz -> quiz.getLesson().getId(), Quiz::getId, (a, b) -> a));
	}

	private void applyMaterial(Material material, MaterialRequest request) {
		material.setName(request.name().trim());
		material.setType(request.type());
		material.setUrl(request.url().trim());
		material.setFileSize(request.fileSize());
		material.setMimeType(trimToNull(request.mimeType()));
	}

	private MaterialResponse toMaterialResponse(Material material) {
		return new MaterialResponse(
				material.getId(), material.getLesson().getId(), material.getName(), material.getType(),
				viewUrl(material), storedKey(material) != null,
				material.getFileSize(), material.getMimeType(), material.getPosition());
	}

	private static Map<UUID, Long> toMap(List<CourseCount> counts) {
		return counts.stream().collect(Collectors.toMap(CourseCount::getCourseId, CourseCount::getTotal));
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
