package com.example.course_service.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.course_service.client.EnrollmentClient;
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


	private static final Map<CourseStatus, List<CourseStatus>> TRANSITIONS = new EnumMap<>(CourseStatus.class);

	static {
		TRANSITIONS.put(CourseStatus.DRAFT, List.of(CourseStatus.PUBLISHED, CourseStatus.ARCHIVED));
		TRANSITIONS.put(CourseStatus.PUBLISHED, List.of(CourseStatus.UNPUBLISHED, CourseStatus.ARCHIVED));
		TRANSITIONS.put(CourseStatus.UNPUBLISHED,
				List.of(CourseStatus.PUBLISHED, CourseStatus.DRAFT, CourseStatus.ARCHIVED));
		TRANSITIONS.put(CourseStatus.ARCHIVED, List.of(CourseStatus.DRAFT));
	}

	private final CourseRepository courseRepository;
	private final EnrollmentClient enrollmentClient;
	private final CategoryRepository categoryRepository;
	private final SectionRepository sectionRepository;
	private final LessonRepository lessonRepository;
	private final MaterialRepository materialRepository;
	private final QuizRepository quizRepository;
	private final StorageService storageService;

	private static final Logger log = LoggerFactory.getLogger(CourseServiceImpl.class);

	private static final String STORED_PREFIX = "minio:";

	private static final long MAX_THUMBNAIL_BYTES = 5L * 1024 * 1024;



	@Override
	public List<CourseSummaryResponse> search(String keyword, CourseStatus status, UUID instructorId, UUID categoryId) {
		if (status != null && status != CourseStatus.PUBLISHED) {
			return List.of();
		}
		List<Course> courses = courseRepository.search(
				toPattern(keyword), CourseStatus.PUBLISHED, instructorId, categoryId);

		List<CourseSummaryResponse> result = new ArrayList<>();
		for (Course course : courses) {
			result.add(toSummary(course));
		}
		return result;
	}

	@Override
	public CourseDetailResponse getDetail(LoginUser user, UUID id) {
		Course course = requireCourse(id);
		if (course.getStatus() == CourseStatus.DRAFT) {
			throw new ResourceNotFoundException("Không tìm thấy khoá học id=" + id);
		}
		return toDetail(course, canViewAllContent(user, course));
	}


	private boolean canViewAllContent(LoginUser user, Course course) {
		if (user.isGuest()) {
			return false;
		}
		if (user.canManage(course.getInstructorId())) {
			return true;
		}
		return enrollmentClient.hasAccess(user.id(), course.getId());
	}

	@Override
	public List<CourseSummaryResponse> searchMine(LoginUser user, String keyword, CourseStatus status, UUID categoryId) {
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
		if (!course.getInstructorId().equals(user.id())) {
			throw new ResourceNotFoundException("Không tìm thấy khoá học id=" + id);
		}
		return toDetail(course, true);
	}

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

		// Bỏ các bài quiz chưa tạo quiz, vì học viên chưa làm được.
		List<Lesson> readyLessons = new ArrayList<>();
		for (Lesson lesson : allLessons) {
			if (lesson.getType() != LessonType.QUIZ || quizIdByLesson.containsKey(lesson.getId())) {
				readyLessons.add(lesson);
			}
		}

		// Gom bài học theo chương: key = id chương, value = danh sách bài học (đã đúng thứ tự).
		Map<UUID, List<PublishedCurriculumResponse.Lesson>> lessonsBySection = new HashMap<>();
		for (Lesson lesson : readyLessons) {
			UUID sectionId = lesson.getSection().getId();
			List<PublishedCurriculumResponse.Lesson> list = lessonsBySection.get(sectionId);
			if (list == null) {
				list = new ArrayList<>();
				lessonsBySection.put(sectionId, list);
			}
			list.add(new PublishedCurriculumResponse.Lesson(
					lesson.getId(), lesson.getTitle(), lesson.getType(),
					lesson.getPosition(), lesson.isPreview(),
					quizIdByLesson.get(lesson.getId())));
		}

		// Chỉ trả về chương có ít nhất 1 bài học.
		List<PublishedCurriculumResponse.Section> sections = new ArrayList<>();
		for (Section section : sectionRepository.findByCourseIdOrderByPositionAscIdAsc(id)) {
			List<PublishedCurriculumResponse.Lesson> sectionLessons = lessonsBySection.get(section.getId());
			if (sectionLessons == null) {
				continue;
			}
			sections.add(new PublishedCurriculumResponse.Section(
					section.getId(), section.getTitle(), section.getPosition(), sectionLessons));
		}

		return new PublishedCurriculumResponse(course.getId(), course.getTitle(), readyLessons.size(), sections);
	}

	@Override
	@Transactional
	public CourseDetailResponse create(LoginUser user, CourseRequest request) {
		UUID instructorId = user.id();
		String title = request.title().trim();

		if (courseRepository.existsByInstructorIdAndTitleIgnoreCase(instructorId, title)) {
			throw new ConflictException("Bạn đã có khoá học tên '" + title + "'. Hãy đặt tên khác.");
		}

		Course course = Course.builder()
				.instructorId(instructorId)
				.title(title)
				.description(trimToNull(request.description()))
				.thumbnailUrl(trimToNull(request.thumbnailUrl()))
				.category(findCategoryOrNull(request.categoryId()))
				.status(CourseStatus.DRAFT)
				.build();
		return toDetail(courseRepository.saveAndFlush(course), true);
	}

	@Override
	@Transactional
	public CourseDetailResponse update(LoginUser user, UUID id, CourseRequest request) {
		Course course = requireManageableCourse(user, id);
		ensureEditable(course);
		String title = request.title().trim();

		if (courseRepository.existsByInstructorIdAndTitleIgnoreCaseAndIdNot(course.getInstructorId(), title, id)) {
			throw new ConflictException("Bạn đã có khoá học tên '" + title + "'. Hãy đặt tên khác.");
		}

		course.setTitle(title);
		course.setDescription(trimToNull(request.description()));
		course.setCategory(findCategoryOrNull(request.categoryId()));

		String newThumbnail = trimToNull(request.thumbnailUrl());
		if (newThumbnail != null) {
			replaceThumbnail(course, newThumbnail);
		}
		return toDetail(courseRepository.saveAndFlush(course), true);
	}

	@Override
	@Transactional
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
		Course course = requireManageableCourse(user, id);
		ensureEditable(course);

		StorageService.StoredFile stored = storageService.upload("courses/" + id + "/thumbnail", file);

		replaceThumbnail(course, STORED_PREFIX + stored.objectKey());
		return toDetail(courseRepository.saveAndFlush(course), true);
	}

	@Override
	@Transactional
	public CourseDetailResponse removeThumbnail(LoginUser user, UUID id) {
		Course course = requireManageableCourse(user, id);
		ensureEditable(course);
		replaceThumbnail(course, null);
		return toDetail(courseRepository.saveAndFlush(course), true);
	}

	private void replaceThumbnail(Course course, String newValue) {
		String oldKey = storedKey(course.getThumbnailUrl());
		course.setThumbnailUrl(newValue);

		if (oldKey == null) {
			return;
		}
		String oldStoredValue = STORED_PREFIX + oldKey;
		if (oldStoredValue.equals(newValue)) {
			return;
		}
		List<String> keysToDelete = new ArrayList<>();
		keysToDelete.add(oldKey);
		deleteStoredFiles(keysToDelete);
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
		return toDetail(courseRepository.saveAndFlush(course), true);
	}

	@Override
	@Transactional
	public void delete(LoginUser user, UUID id) {
		Course course = requireManageableCourse(user, id);
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


	@Override
	@Transactional
	public CourseDetailResponse addSection(LoginUser user, UUID courseId, SectionRequest request) {
		Course course = requireManageableCourse(user, courseId);
		ensureEditable(course);
		String title = request.title().trim();

		if (sectionRepository.existsByCourseIdAndTitleIgnoreCase(courseId, title)) {
			throw new ConflictException("Khoá học đã có chương tên '" + title + "'. Hãy đặt tên khác.");
		}

		sectionRepository.save(Section.builder()
				.course(course)
				.title(title)
				.description(trimToNull(request.description()))
				.position(sectionRepository.findMaxPosition(courseId) + 1)
				.build());
		return refreshed(courseId);
	}

	@Override
	@Transactional
	public CourseDetailResponse updateSection(LoginUser user, UUID sectionId, SectionRequest request) {
		Section section = requireEditableSection(user, sectionId);
		String title = request.title().trim();

		UUID courseId = section.getCourse().getId();
		if (sectionRepository.existsByCourseIdAndTitleIgnoreCaseAndIdNot(courseId, title, sectionId)) {
			throw new ConflictException("Khoá học đã có chương tên '" + title + "'. Hãy đặt tên khác.");
		}

		section.setTitle(title);
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

		// Đánh lại số thứ tự 1, 2, 3… cho các chương còn lại.
		List<Section> remaining = sectionRepository.findByCourseIdOrderByPositionAscIdAsc(courseId);
		for (int i = 0; i < remaining.size(); i++) {
			remaining.get(i).setPosition(i + 1);
		}
		return refreshed(courseId);
	}

	@Override
	@Transactional
	public CourseDetailResponse reorderSections(LoginUser user, UUID courseId, List<UUID> orderedIds) {
		Course course = requireManageableCourse(user, courseId);
		ensureEditable(course);

		List<Section> sections = sectionRepository.findByCourseIdOrderByPositionAscIdAsc(courseId);

		Map<UUID, Section> sectionById = new HashMap<>();
		for (Section section : sections) {
			sectionById.put(section.getId(), section);
		}
		checkOrderedIds(sectionById.keySet(), orderedIds, "chương");

		for (int i = 0; i < orderedIds.size(); i++) {
			sectionById.get(orderedIds.get(i)).setPosition(i + 1);
		}
		return refreshed(courseId);
	}


	@Override
	@Transactional
	public CourseDetailResponse addLesson(LoginUser user, UUID sectionId, LessonRequest request) {
		Section section = requireEditableSection(user, sectionId);
		String title = request.title().trim();

		if (lessonRepository.existsBySectionIdAndTitleIgnoreCase(sectionId, title)) {
			throw new ConflictException("Chương đã có bài học tên '" + title + "'. Hãy đặt tên khác.");
		}

		lessonRepository.save(Lesson.builder()
				.section(section)
				.title(title)
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

		String title = request.title().trim();
		UUID sectionId = lesson.getSection().getId();
		if (lessonRepository.existsBySectionIdAndTitleIgnoreCaseAndIdNot(sectionId, title, lessonId)) {
			throw new ConflictException("Chương đã có bài học tên '" + title + "'. Hãy đặt tên khác.");
		}

		lesson.setTitle(title);
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
		// Đánh lại số thứ tự 1, 2, 3… cho các bài học còn lại trong chương.
		List<Lesson> remaining = lessonRepository.findBySectionIdOrderByPositionAscIdAsc(sectionId);
		for (int i = 0; i < remaining.size(); i++) {
			remaining.get(i).setPosition(i + 1);
		}
		return refreshed(courseId);
	}

	@Override
	@Transactional
	public CourseDetailResponse reorderLessons(LoginUser user, UUID sectionId, List<UUID> orderedIds) {
		Section section = requireEditableSection(user, sectionId);
		List<Lesson> lessons = lessonRepository.findBySectionIdOrderByPositionAscIdAsc(sectionId);

		Map<UUID, Lesson> lessonById = new HashMap<>();
		for (Lesson lesson : lessons) {
			lessonById.put(lesson.getId(), lesson);
		}
		checkOrderedIds(lessonById.keySet(), orderedIds, "bài học");

		for (int i = 0; i < orderedIds.size(); i++) {
			lessonById.get(orderedIds.get(i)).setPosition(i + 1);
		}
		return refreshed(section.getCourse().getId());
	}


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


	@Override
	@Transactional
	public CourseDetailResponse uploadMaterial(LoginUser user, UUID lessonId, MultipartFile file, String name, MaterialType type) {
		if (file == null || file.isEmpty()) {
			throw new BusinessRuleException("File upload đang trống");
		}
		Lesson lesson = requireEditableLesson(user, lessonId);
		UUID courseId = lesson.getSection().getCourse().getId();

		StorageService.StoredFile stored = storageService.upload(
				"courses/" + courseId + "/lessons/" + lessonId, file);

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

		MaterialType materialType;
		if (type != null) {
			materialType = type;
		}
		else {
			materialType = guessType(stored.contentType());
		}

		materialRepository.saveAndFlush(Material.builder()
				.lesson(lesson)
				.name(truncate(displayName, 255))
				.type(materialType)
				.url(STORED_PREFIX + stored.objectKey())
				.fileSize(stored.size())
				.mimeType(truncate(stored.contentType(), 100))
				.position(materialRepository.findMaxPosition(lessonId) + 1)
				.build());
		return refreshed(courseId);
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
				deleteStoredFiles(List.of(oldStoredKey));
			}
		}
		return refreshed(material.getLesson().getSection().getCourse().getId());
	}

	@Override
	@Transactional
	public CourseDetailResponse replaceMaterialFile(LoginUser user, UUID materialId, MultipartFile file, String name,
			MaterialType type) {
		if (file == null || file.isEmpty()) {
			throw new BusinessRuleException("File upload đang trống");
		}
		// Kiểm tra quyền & trạng thái trước khi upload để không đẩy file lên MinIO vô ích.
		Material material = requireEditableMaterial(user, materialId);
		Lesson lesson = material.getLesson();
		String folder = "courses/" + lesson.getSection().getCourse().getId() + "/lessons/" + lesson.getId();

		StorageService.StoredFile stored = storageService.upload(folder, file);
		String oldStoredKey = storedKey(material);

		material.setUrl(STORED_PREFIX + stored.objectKey());
		material.setFileSize(stored.size());
		material.setMimeType(truncate(stored.contentType(), 100));
		if (trimToNull(name) != null) {
			material.setName(truncate(name.trim(), 255));
		}
		if (type != null) {
			material.setType(type);
		}
		materialRepository.saveAndFlush(material);

		if (oldStoredKey != null && !oldStoredKey.equals(stored.objectKey())) {
			deleteStoredFiles(List.of(oldStoredKey));
		}
		return refreshed(lesson.getSection().getCourse().getId());
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
			deleteStoredFiles(List.of(storedKey));
		}
		// Đánh lại số thứ tự 1, 2, 3… cho các học liệu còn lại trong bài học.
		List<Material> remaining = materialRepository.findByLessonIdOrderByPositionAscIdAsc(lesson.getId());
		for (int i = 0; i < remaining.size(); i++) {
			remaining.get(i).setPosition(i + 1);
		}
		return refreshed(courseId);
	}


	private static List<CourseStatus> allowedTransitions(CourseStatus from) {
		return TRANSITIONS.getOrDefault(from, List.of());
	}

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
		// Đếm số bài học của từng chương: key = id chương, value = số bài học.
		Map<UUID, Integer> lessonsPerSection = new HashMap<>();
		for (Lesson lesson : lessons) {
			UUID sectionId = lesson.getSection().getId();
			Integer count = lessonsPerSection.get(sectionId);
			if (count == null) {
				count = 0;
			}
			lessonsPerSection.put(sectionId, count + 1);
		}

		// Chương nào không có trong map nghĩa là chưa có bài học nào.
		for (Section section : sections) {
			Integer count = lessonsPerSection.get(section.getId());
			if (count == null || count == 0) {
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


	// ============================================================ tìm & kiểm tra

	/** Như {@link #requireCourse} nhưng chặn nếu người gọi không phải giảng viên phụ trách. */
	private Course requireManageableCourse(LoginUser user, UUID id) {
		Course course = requireCourse(id);
		checkCanManage(user, course);
		return course;
	}

	private Course requireCourse(UUID id) {
		Optional<Course> found = courseRepository.findById(id);
		if (found.isEmpty()) {
			throw new ResourceNotFoundException("Không tìm thấy khoá học id=" + id);
		}
		return found.get();
	}

	private Section requireEditableSection(LoginUser user, UUID sectionId) {
		Optional<Section> found = sectionRepository.findByIdWithCourse(sectionId);
		if (found.isEmpty()) {
			throw new ResourceNotFoundException("Không tìm thấy chương id=" + sectionId);
		}
		Section section = found.get();
		checkCanManage(user, section.getCourse());
		ensureEditable(section.getCourse());
		return section;
	}

	private Lesson requireEditableLesson(LoginUser user, UUID lessonId) {
		Optional<Lesson> found = lessonRepository.findByIdWithContext(lessonId);
		if (found.isEmpty()) {
			throw new ResourceNotFoundException("Không tìm thấy bài học id=" + lessonId);
		}
		Lesson lesson = found.get();
		checkCanManage(user, lesson.getSection().getCourse());
		ensureEditable(lesson.getSection().getCourse());
		return lesson;
	}

	private Material requireEditableMaterial(LoginUser user, UUID materialId) {
		Optional<Material> found = materialRepository.findByIdWithContext(materialId);
		if (found.isEmpty()) {
			throw new ResourceNotFoundException("Không tìm thấy học liệu id=" + materialId);
		}
		Material material = found.get();
		checkCanManage(user, material.getLesson().getSection().getCourse());
		ensureEditable(material.getLesson().getSection().getCourse());
		return material;
	}


	private void deleteLessons(Collection<Lesson> lessons) {
		if (lessons.isEmpty()) {
			return;
		}
		List<UUID> lessonIds = new ArrayList<>();
		for (Lesson lesson : lessons) {
			lessonIds.add(lesson.getId());
		}
		List<Material> materials = materialRepository.findByLessonIdIn(lessonIds);
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
		deleteStoredFiles(storedKeys);
	}

	// ============================================================ file MinIO

	/** Khoá object trên MinIO nếu học liệu là file đã upload, ngược lại null (học liệu dạng link). */
	private static String storedKey(Material material) {
		return storedKey(material.getUrl());
	}

	private static String storedKey(String storedValue) {
		if (storedValue == null) {
			return null;
		}
		if (storedValue.startsWith(STORED_PREFIX)) {
			return storedValue.substring(STORED_PREFIX.length());
		}
		return null;
	}

	private void deleteStoredFiles(List<String> objectKeys) {
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


	/** Danh sách id gửi lên phải không bị lặp và gồm đúng, đủ các id đang có. */
	private static void checkOrderedIds(Set<UUID> existing, List<UUID> orderedIds, String label) {
		Set<UUID> requested = new HashSet<>(orderedIds);
		if (requested.size() != orderedIds.size()) {
			throw new BusinessRuleException("Danh sách " + label + " có id bị lặp");
		}
		if (!requested.equals(existing)) {
			throw new BusinessRuleException(
					"Danh sách " + label + " phải gồm đúng và đủ các id hiện có: " + existing);
		}
	}


	private CourseDetailResponse refreshed(UUID courseId) {
		sectionRepository.flush();
		return toDetail(requireCourse(courseId), true);
	}

	/**
	 * @param fullAccess true = người xem được xem học liệu của mọi bài học;
	 *                   false = chỉ bài "Xem thử" có link, các bài khác bị khoá (url = null)
	 */
	private CourseDetailResponse toDetail(Course course, boolean fullAccess) {
		UUID courseId = course.getId();
		List<Section> sections = sectionRepository.findByCourseIdOrderByPositionAscIdAsc(courseId);
		List<Lesson> lessons = lessonRepository.findByCourseIdOrdered(courseId);
		Map<UUID, UUID> quizIdByLesson = quizIdsByLesson(lessons);

		Map<UUID, List<Material>> materialsByLesson = new HashMap<>();
		for (Material material : materialRepository.findByCourseIdOrdered(courseId)) {
			UUID lessonId = material.getLesson().getId();
			List<Material> list = materialsByLesson.get(lessonId);
			if (list == null) {
				list = new ArrayList<>();
				materialsByLesson.put(lessonId, list);
			}
			list.add(material);
		}

		Map<UUID, List<LessonResponse>> lessonsBySection = new HashMap<>();
		for (Lesson lesson : lessons) {
			boolean locked = !fullAccess && !lesson.isPreview();

			List<MaterialResponse> materials = new ArrayList<>();
			List<Material> lessonMaterials = materialsByLesson.get(lesson.getId());
			if (lessonMaterials != null) {
				for (Material material : lessonMaterials) {
					materials.add(toMaterialResponse(material, locked));
				}
			}

			UUID sectionId = lesson.getSection().getId();
			List<LessonResponse> sectionLessons = lessonsBySection.get(sectionId);
			if (sectionLessons == null) {
				sectionLessons = new ArrayList<>();
				lessonsBySection.put(sectionId, sectionLessons);
			}
			sectionLessons.add(new LessonResponse(
							lesson.getId(), lesson.getSection().getId(), lesson.getTitle(), lesson.getDescription(),
							lesson.getType(), lesson.getPosition(), lesson.isPreview(), locked,
							quizIdByLesson.get(lesson.getId()),
							materials));
		}

		List<SectionResponse> sectionResponses = new ArrayList<>();
		for (Section section : sections) {
			List<LessonResponse> sectionLessons = lessonsBySection.get(section.getId());
			if (sectionLessons == null) {
				sectionLessons = List.of();
			}
			sectionResponses.add(new SectionResponse(
					section.getId(), courseId, section.getTitle(), section.getDescription(),
					section.getPosition(), sectionLessons));
		}

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
		List<UUID> lessonIds = new ArrayList<>();
		for (Lesson lesson : lessons) {
			lessonIds.add(lesson.getId());
		}

		// key = id bài học, value = id quiz của bài đó.
		Map<UUID, UUID> result = new HashMap<>();
		for (Quiz quiz : quizRepository.findByLessonIdIn(lessonIds)) {
			UUID lessonId = quiz.getLesson().getId();
			if (!result.containsKey(lessonId)) {
				result.put(lessonId, quiz.getId());
			}
		}
		return result;
	}

	private void applyMaterial(Material material, MaterialRequest request) {
		material.setName(request.name().trim());
		material.setType(request.type());
		material.setUrl(request.url().trim());
		material.setFileSize(request.fileSize());
		material.setMimeType(trimToNull(request.mimeType()));
	}

	/** locked = true → không trả link xem (chỉ còn tên / loại để học viên biết bài có gì). */
	private MaterialResponse toMaterialResponse(Material material, boolean locked) {
		String url = null;
		if (!locked) {
			url = viewUrl(material);
		}
		return new MaterialResponse(
				material.getId(), material.getLesson().getId(), material.getName(), material.getType(),
				url, storedKey(material) != null,
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
