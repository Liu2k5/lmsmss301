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
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
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
import com.example.course_service.entity.Category;
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
import com.example.course_service.repository.CategoryRepository;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.repository.MaterialRepository;
import com.example.course_service.repository.QuizRepository;
import com.example.course_service.repository.SectionRepository;
import com.example.course_service.service.CourseService;
import com.example.course_service.service.StorageService;
import com.example.course_service.security.LoginUser;

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
	private final CategoryRepository categoryRepository;
	private final SectionRepository sectionRepository;
	private final LessonRepository lessonRepository;
	private final MaterialRepository materialRepository;
	private final QuizRepository quizRepository;
	private final StorageService storageService;
	private final PlatformTransactionManager transactionManager;

	private static final Logger log = LoggerFactory.getLogger(CourseServiceImpl.class);

	/** Học liệu upload lên MinIO lưu cột url dạng {@code minio:<object key>}; link xem được sinh khi đọc. */
	private static final String STORED_PREFIX = "minio:";

	private static final long MAX_THUMBNAIL_BYTES = 5L * 1024 * 1024;



	@Override
	public List<CourseSummaryResponse> search(String keyword, CourseStatus status, UUID instructorId, UUID categoryId) {
		List<Course> courses = courseRepository.search(toPattern(keyword), status, instructorId, categoryId);

		List<CourseSummaryResponse> result = new ArrayList<>();
		for (Course course : courses) {
			if (course.getStatus() == CourseStatus.DRAFT) {
				continue;
			}
			result.add(toSummary(course));
		}
		return result;
	}

	@Override
	public CourseDetailResponse getDetail(UUID id) {
		Course course = requireCourse(id);
		// Khoá nháp: coi như không tồn tại với API công khai.
		if (course.getStatus() == CourseStatus.DRAFT) {
			throw new ResourceNotFoundException("Không tìm thấy khoá học id=" + id);
		}
		return toDetail(course);
	}

	@Override
	public List<CourseSummaryResponse> searchMine(LoginUser user, String keyword, CourseStatus status, UUID categoryId) {
		// Chỉ lấy khoá đứng tên giảng viên đang đăng nhập → bản nháp của mình cũng hiện.
		List<Course> courses = courseRepository.search(toPattern(keyword), status, user.id(), categoryId);

		List<CourseSummaryResponse> result = new ArrayList<>();
		for (Course course : courses) {
			result.add(toSummary(course));
		}
		return result;
	}

	@Override
	public CourseDetailResponse getMyDetail(LoginUser user, UUID id) {
		Course course = requireCourse(id);
		// Khoá của giảng viên khác: coi như không tồn tại.
		if (!course.getInstructorId().equals(user.id())) {
			throw new ResourceNotFoundException("Không tìm thấy khoá học id=" + id);
		}
		return toDetail(course);
	}

	/** Từ khoá tìm kiếm → mẫu LIKE; để trống → null (không lọc). */
	private static String toPattern(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return null;
		}
		return "%" + keyword.trim().toLowerCase() + "%";
	}

	private CourseSummaryResponse toSummary(Course course) {
		return new CourseSummaryResponse(
				course.getId(),
				course.getInstructorId(),
				course.getTitle(),
				course.getDescription(),
				viewUrl(course.getThumbnailUrl()),
				course.getStatus(),
				categoryIdOf(course),
				categoryNameOf(course),
				course.getCreatedAt(),
				course.getUpdatedAt());
	}

	@Override
	public PublishedCurriculumResponse getPublishedCurriculum(UUID id) {
		Course course = requireCourse(id);
		if (course.getStatus() != CourseStatus.PUBLISHED) {
			throw new ResourceNotFoundException("Khoá học id=" + id + " chưa được phát hành");
		}

		List<Lesson> allLessons = lessonRepository.findByCourseIdOrdered(id);
		Map<UUID, UUID> quizIdByLesson = quizIdsByLesson(allLessons);

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
	public CourseDetailResponse create(LoginUser user, CourseRequest request) {
		checkCanAssignInstructor(user, request.instructorId());
		Course course = Course.builder()
				.instructorId(request.instructorId())
				.title(request.title().trim())
				.description(trimToNull(request.description()))
				.thumbnailUrl(trimToNull(request.thumbnailUrl()))
				.category(findCategoryOrNull(request.categoryId()))
				.status(CourseStatus.DRAFT)
				.build();
		return toDetail(courseRepository.saveAndFlush(course));
	}

	@Override
	@Transactional
	public CourseDetailResponse update(LoginUser user, UUID id, CourseRequest request) {
		Course course = requireManageableCourse(user, id);
		ensureEditable(course);
		checkCanAssignInstructor(user, request.instructorId());
		course.setInstructorId(request.instructorId());
		course.setTitle(request.title().trim());
		course.setDescription(trimToNull(request.description()));
		course.setCategory(findCategoryOrNull(request.categoryId()));

		String newThumbnail = trimToNull(request.thumbnailUrl());
		if (newThumbnail != null) {
			replaceThumbnail(course, newThumbnail);
		}
		return toDetail(courseRepository.saveAndFlush(course));
	}

	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public CourseDetailResponse uploadThumbnail(LoginUser user, UUID id, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessRuleException("File ảnh bìa đang trống");
		}
		if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
			throw new BusinessRuleException("Ảnh bìa phải là file hình ảnh (JPG, PNG, WEBP…)");
		}
		if (file.getSize() > MAX_THUMBNAIL_BYTES) {
			throw new BusinessRuleException("Ảnh bìa tối đa 5 MB");
		}
		readTransaction().executeWithoutResult(status -> ensureEditable(requireManageableCourse(user, id)));

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
	public CourseDetailResponse removeThumbnail(LoginUser user, UUID id) {
		Course course = requireManageableCourse(user, id);
		ensureEditable(course);
		replaceThumbnail(course, null);
		return toDetail(courseRepository.saveAndFlush(course));
	}

	/** Đổi ảnh bìa; nếu ảnh cũ nằm trên MinIO thì xoá file cũ sau khi commit. */
	private void replaceThumbnail(Course course, String newValue) {
		// Lấy key của ảnh cũ (null nếu ảnh cũ là link ngoài hoặc chưa có ảnh)
		String oldKey = storedKey(course.getThumbnailUrl());
		course.setThumbnailUrl(newValue);

		// Ảnh cũ không nằm trên MinIO → không có gì để xoá
		if (oldKey == null) {
			return;
		}
		// Ảnh mới vẫn là đúng file cũ → không xoá
		String oldStoredValue = STORED_PREFIX + oldKey;
		if (oldStoredValue.equals(newValue)) {
			return;
		}
		// Ảnh đã đổi → xoá file cũ trên MinIO (sau khi DB commit)
		List<String> keysToDelete = new ArrayList<>();
		keysToDelete.add(oldKey);
		deleteStoredFilesAfterCommit(keysToDelete);
	}

	@Override
	@Transactional
	public CourseDetailResponse changeStatus(LoginUser user, UUID id, CourseStatus target) {
		Course course = requireManageableCourse(user, id);
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
	public void delete(LoginUser user, UUID id) {
		Course course = requireManageableCourse(user, id);
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
	public CourseDetailResponse addSection(LoginUser user, UUID courseId, SectionRequest request) {
		Course course = requireManageableCourse(user, courseId);
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
	public CourseDetailResponse updateSection(LoginUser user, UUID sectionId, SectionRequest request) {
		Section section = requireEditableSection(user, sectionId);
		section.setTitle(request.title().trim());
		section.setDescription(trimToNull(request.description()));
		return refreshed(section.getCourse().getId());
	}

	@Override
	@Transactional
	public CourseDetailResponse deleteSection(LoginUser user, UUID sectionId) {
		Section section = requireEditableSection(user, sectionId);
		UUID courseId = section.getCourse().getId();

		deleteLessons(lessonRepository.findBySectionIdOrderByPositionAscIdAsc(sectionId));
		sectionRepository.delete(section);
		sectionRepository.flush();

		renumber(sectionRepository.findByCourseIdOrderByPositionAscIdAsc(courseId), Section::setPosition);
		return refreshed(courseId);
	}

	@Override
	@Transactional
	public CourseDetailResponse reorderSections(LoginUser user, UUID courseId, List<UUID> orderedIds) {
		Course course = requireManageableCourse(user, courseId);
		ensureEditable(course);

		List<Section> sections = sectionRepository.findByCourseIdOrderByPositionAscIdAsc(courseId);
		applyOrder(sections, Section::getId, Section::setPosition, orderedIds, "chương");
		return refreshed(courseId);
	}

	// ================================================================= bài học

	@Override
	@Transactional
	public CourseDetailResponse addLesson(LoginUser user, UUID sectionId, LessonRequest request) {
		Section section = requireEditableSection(user, sectionId);

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
	public CourseDetailResponse updateLesson(LoginUser user, UUID lessonId, LessonRequest request) {
		Lesson lesson = requireEditableLesson(user, lessonId);

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
	public CourseDetailResponse deleteLesson(LoginUser user, UUID lessonId) {
		Lesson lesson = requireEditableLesson(user, lessonId);
		UUID sectionId = lesson.getSection().getId();
		UUID courseId = lesson.getSection().getCourse().getId();

		deleteLessons(List.of(lesson));
		renumber(lessonRepository.findBySectionIdOrderByPositionAscIdAsc(sectionId), Lesson::setPosition);
		return refreshed(courseId);
	}

	@Override
	@Transactional
	public CourseDetailResponse reorderLessons(LoginUser user, UUID sectionId, List<UUID> orderedIds) {
		Section section = requireEditableSection(user, sectionId);
		List<Lesson> lessons = lessonRepository.findBySectionIdOrderByPositionAscIdAsc(sectionId);
		applyOrder(lessons, Lesson::getId, Lesson::setPosition, orderedIds, "bài học");
		return refreshed(section.getCourse().getId());
	}

	// ================================================================ học liệu

	@Override
	@Transactional
	public CourseDetailResponse addMaterial(LoginUser user, UUID lessonId, MaterialRequest request) {
		Lesson lesson = requireEditableLesson(user, lessonId);
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
	public CourseDetailResponse uploadMaterial(LoginUser user, UUID lessonId, MultipartFile file, String name, MaterialType type) {
		if (file == null || file.isEmpty()) {
			throw new BusinessRuleException("File upload đang trống");
		}
		UUID courseId = readTransaction().execute(
				status -> requireEditableLesson(user, lessonId).getSection().getCourse().getId());

		StorageService.StoredFile stored = storageService.upload(
				"courses/" + courseId + "/lessons/" + lessonId, file);

		// Tên hiển thị: ưu tiên tên người dùng nhập → tên file gốc → "Học liệu"
		String displayName;
		if (trimToNull(name) != null) {
			displayName = name.trim();
		}
		else if (trimToNull(file.getOriginalFilename()) != null) {
			displayName = file.getOriginalFilename().trim();
		}
		else {
			displayName = "Học liệu";
		}

		// Loại học liệu: người dùng chọn thì dùng, không chọn thì đoán theo MIME type của file
		MaterialType materialType;
		if (type != null) {
			materialType = type;
		}
		else {
			materialType = guessType(stored.contentType());
		}

		return saveOrDiscardUpload(stored, () -> {
			// Kiểm tra lại: bài học có thể vừa bị xoá / khoá học vừa bị lưu trữ trong lúc upload.
			Lesson lesson = requireEditableLesson(user, lessonId);
			materialRepository.saveAndFlush(Material.builder()
					.lesson(lesson)
					.name(truncate(displayName, 255))
					.type(materialType)
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
	public CourseDetailResponse updateMaterial(LoginUser user, UUID materialId, MaterialRequest request) {
		Material material = requireEditableMaterial(user, materialId);
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
	public CourseDetailResponse deleteMaterial(LoginUser user, UUID materialId) {
		Material material = requireEditableMaterial(user, materialId);
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

	// ============================================================ kiểm tra quyền

	/** Chỉ giảng viên phụ trách khoá học mới được sửa nội dung khoá học. */
	private static void checkCanManage(LoginUser user, Course course) {
		if (!user.canManage(course.getInstructorId())) {
			throw new AccessDeniedException("Chỉ giảng viên phụ trách khoá học mới được chỉnh sửa nội dung khoá học này");
		}
	}

	/** Giảng viên chỉ được tạo / sửa khoá học đứng tên chính mình. */
	private static void checkCanAssignInstructor(LoginUser user, UUID instructorId) {
		if (!instructorId.equals(user.id())) {
			throw new AccessDeniedException("Mã giảng viên phải là tài khoản đang đăng nhập");
		}
	}

	// ============================================================ tìm & kiểm tra

	/** Như {@link #requireCourse} nhưng chặn nếu người gọi không phải giảng viên phụ trách. */
	private Course requireManageableCourse(LoginUser user, UUID id) {
		Course course = requireCourse(id);
		checkCanManage(user, course);
		return course;
	}

	private Course requireCourse(UUID id) {
		return courseRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khoá học id=" + id));
	}

	private Section requireEditableSection(LoginUser user, UUID sectionId) {
		Section section = sectionRepository.findByIdWithCourse(sectionId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương id=" + sectionId));
		checkCanManage(user, section.getCourse());
		ensureEditable(section.getCourse());
		return section;
	}

	private Lesson requireEditableLesson(LoginUser user, UUID lessonId) {
		Lesson lesson = lessonRepository.findByIdWithContext(lessonId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học id=" + lessonId));
		checkCanManage(user, lesson.getSection().getCourse());
		ensureEditable(lesson.getSection().getCourse());
		return lesson;
	}

	private Material requireEditableMaterial(LoginUser user, UUID materialId) {
		Material material = materialRepository.findByIdWithContext(materialId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học liệu id=" + materialId));
		checkCanManage(user, material.getLesson().getSection().getCourse());
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
		// Gom key của những học liệu là file trên MinIO (bỏ qua học liệu dạng link)
		List<String> storedKeys = new ArrayList<>();
		for (Material material : materials) {
			String key = storedKey(material);
			if (key != null) {
				storedKeys.add(key);
			}
		}

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
		// Không có giá trị → không phải file
		if (storedValue == null) {
			return null;
		}
		// Có tiền tố "minio:" → là file đã upload, bỏ tiền tố để lấy key
		// Ví dụ: "minio:courses/abc/thumb.png" → "courses/abc/thumb.png"
		if (storedValue.startsWith(STORED_PREFIX)) {
			return storedValue.substring(STORED_PREFIX.length());
		}
		// Không có tiền tố → là link ngoài (YouTube, Google Drive…)
		return null;
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
		// Người gọi đã qua kiểm tra quyền sửa nên không cần kiểm tra quyền xem nháp nữa
		return toDetail(requireCourse(courseId));
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
				categoryIdOf(course), categoryNameOf(course),
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

	// ============================================================ danh mục

	/** categoryId null → khoá học không có danh mục; có id mà không tìm thấy → 404. */
	private Category findCategoryOrNull(UUID categoryId) {
		if (categoryId == null) {
			return null;
		}
		Optional<Category> found = categoryRepository.findById(categoryId);
		if (found.isEmpty()) {
			throw new ResourceNotFoundException("Không tìm thấy danh mục id=" + categoryId);
		}
		return found.get();
	}

	private static UUID categoryIdOf(Course course) {
		Category category = course.getCategory();
		if (category == null) {
			return null;
		}
		return category.getId();
	}

	private static String categoryNameOf(Course course) {
		Category category = course.getCategory();
		if (category == null) {
			return null;
		}
		return category.getName();
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
