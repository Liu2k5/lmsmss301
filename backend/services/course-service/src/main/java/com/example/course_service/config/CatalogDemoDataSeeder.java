package com.example.course_service.config;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.course_service.entity.Category;
import com.example.course_service.entity.Course;
import com.example.course_service.entity.CourseStatus;
import com.example.course_service.entity.Lesson;
import com.example.course_service.entity.LessonType;
import com.example.course_service.entity.Material;
import com.example.course_service.entity.MaterialType;
import com.example.course_service.entity.Option;
import com.example.course_service.entity.Question;
import com.example.course_service.entity.QuestionType;
import com.example.course_service.entity.Quiz;
import com.example.course_service.entity.Section;
import com.example.course_service.repository.CategoryRepository;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.repository.MaterialRepository;
import com.example.course_service.repository.QuizRepository;
import com.example.course_service.repository.SectionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Bộ khoá học mẫu lớn hơn để thử danh mục, curriculum, học liệu (video / ảnh / tài liệu / bài đọc) và quiz.
 * Tất cả thuộc giảng viên {@code instructor1}. Mỗi khoá chỉ tạo khi chưa có khoá cùng tên, nên khởi động lại
 * nhiều lần không bị trùng. Tắt bằng SEED_DEMO_DATA=false.
 *
 * <p>Học liệu dùng link công khai (không lưu trên MinIO) nên mở được ngay mà không cần upload.
 * Trong quiz, đáp án bắt đầu bằng {@code *} là đáp án đúng.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
@Transactional
public class CatalogDemoDataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(CatalogDemoDataSeeder.class);

	/** {@code instructor1} trong realm Keycloak. */
	private static final UUID INSTRUCTOR_1 = UUID.fromString("22222222-2222-2222-2222-222222222222");

	/** Video mẫu công khai (mp4 ngắn, giấy phép mở) để bấm link là xem được ngay. */
	private static final String VIDEO_BUNNY = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4";
	private static final String VIDEO_SINTEL = "https://test-videos.co.uk/vids/sintel/mp4/h264/360/Sintel_360_10s_1MB.mp4";
	private static final String VIDEO_JELLYFISH = "https://test-videos.co.uk/vids/jellyfish/mp4/h264/360/Jellyfish_360_10s_1MB.mp4";
	private static final String VIDEO_FLOWER = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4";

	private final CourseRepository courseRepository;
	private final CategoryRepository categoryRepository;
	private final SectionRepository sectionRepository;
	private final LessonRepository lessonRepository;
	private final MaterialRepository materialRepository;
	private final QuizRepository quizRepository;

	@Override
	public void run(ApplicationArguments args) {
		seedIfMissing("Python cho người mới bắt đầu", this::seedPython);
		seedIfMissing("SQL và PostgreSQL thực chiến", this::seedSql);
		seedIfMissing("JavaScript hiện đại (ES6+)", this::seedJavaScript);
		seedIfMissing("Git và GitHub cho làm việc nhóm", this::seedGit);
		seedIfMissing("Phân tích dữ liệu với Pandas", this::seedPandas);
		seedIfMissing("Thiết kế UI/UX cơ bản với Figma", this::seedUiUx);
	}

	private void seedIfMissing(String title, java.util.function.Function<String, Course> seeder) {
		if (courseRepository.existsByTitle(title)) {
			return;
		}
		Course course = seeder.apply(title);
		log.info("Đã tạo khoá học mẫu '{}'", course.getTitle());
	}

	// ------------------------------------------------------------------ Python

	private Course seedPython(String title) {
		Course course = course(title,
				"Học Python từ con số 0: biến, kiểu dữ liệu, điều kiện, vòng lặp, hàm và làm việc với file. "
						+ "Phù hợp sinh viên năm nhất và người chuyển ngành.",
				"python", category("Lập trình Backend", "Xây dựng API và xử lý phía server"), CourseStatus.PUBLISHED);

		Section s1 = section(course, "Chương 1: Làm quen Python", "Cài đặt môi trường và viết chương trình đầu tiên.", 1);
		Lesson l1 = lesson(s1, "Cài đặt Python và VS Code", "Hướng dẫn cài Python 3.12, VS Code và extension Python.",
				LessonType.VIDEO, 1, true);
		video(l1, "Video: Cài đặt môi trường", VIDEO_BUNNY, 1);
		image(l1, "Ảnh: Màn hình VS Code sau khi cài", "python-vscode", 2);
		Lesson l2 = lesson(s1, "Biến và kiểu dữ liệu", """
				Python là ngôn ngữ kiểu động: bạn không cần khai báo kiểu khi tạo biến.

				- int: số nguyên, ví dụ age = 20
				- float: số thực, ví dụ gpa = 3.6
				- str: chuỗi, ví dụ name = "An"
				- bool: True / False

				Dùng hàm type(x) để xem kiểu của một biến, và int(), float(), str() để ép kiểu.
				Lưu ý: chuỗi trong Python là bất biến (immutable).""", LessonType.ARTICLE, 2, true);
		document(l2, "Tài liệu chính thức: Python Tutorial", "https://docs.python.org/3/tutorial/introduction.html",
				"text/html", 1);
		Lesson l3 = lesson(s1, "Nhập xuất dữ liệu với input() và print()", null, LessonType.VIDEO, 3, false);
		video(l3, "Video: input() và print()", VIDEO_SINTEL, 1);
		quiz(s1, "Kiểm tra chương 1: Cơ bản về Python", 4, "70.00", 3,
				q(QuestionType.SINGLE_CHOICE, "Hàm nào dùng để in ra màn hình trong Python 3?",
						"*print()", "echo()", "printf()", "console.log()"),
				q(QuestionType.SINGLE_CHOICE, "Kết quả của type(3.0) là gì?",
						"*<class 'float'>", "<class 'int'>", "<class 'str'>", "<class 'double'>"),
				q(QuestionType.TRUE_FALSE, "Chuỗi (str) trong Python có thể sửa trực tiếp từng ký tự.",
						"Đúng", "*Sai"),
				q(QuestionType.MULTIPLE_CHOICE, "Những tên biến nào hợp lệ trong Python? (chọn nhiều)",
						"*student_name", "*_count", "2nd_value", "*totalScore"));

		Section s2 = section(course, "Chương 2: Cấu trúc điều khiển", "Câu lệnh if, vòng lặp for / while.", 2);
		Lesson l4 = lesson(s2, "Câu lệnh if / elif / else", """
				Python dùng thụt lề (4 dấu cách) thay cho dấu ngoặc nhọn để xác định khối lệnh.

				if score >= 8:
				    print("Giỏi")
				elif score >= 6.5:
				    print("Khá")
				else:
				    print("Trung bình")

				Toán tử so sánh: ==, !=, <, >, <=, >=. Kết hợp điều kiện bằng and, or, not.""",
				LessonType.ARTICLE, 1, false);
		image(l4, "Sơ đồ luồng if / elif / else", "python-if-flow", 1);
		Lesson l5 = lesson(s2, "Vòng lặp for và range()", null, LessonType.VIDEO, 2, false);
		video(l5, "Video: Vòng lặp for", VIDEO_JELLYFISH, 1);
		document(l5, "Bài tập vòng lặp (PDF)", "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
				"application/pdf", 2);
		Lesson l6 = lesson(s2, "Vòng lặp while, break và continue", """
				while lặp khi điều kiện còn đúng. break thoát vòng lặp ngay, continue bỏ qua phần còn lại
				của lần lặp hiện tại. Luôn đảm bảo điều kiện sẽ trở thành False để tránh vòng lặp vô hạn.""",
				LessonType.ARTICLE, 3, false);
		document(l6, "W3Schools: Python While Loops", "https://www.w3schools.com/python/python_while_loops.asp",
				"text/html", 1);
		quiz(s2, "Kiểm tra chương 2: Điều kiện và vòng lặp", 4, "60.00", 5,
				q(QuestionType.SINGLE_CHOICE, "list(range(1, 5)) trả về gì?",
						"*[1, 2, 3, 4]", "[1, 2, 3, 4, 5]", "[0, 1, 2, 3, 4]", "[2, 3, 4, 5]"),
				q(QuestionType.SINGLE_CHOICE, "Từ khoá nào bỏ qua phần còn lại của lần lặp hiện tại?",
						"*continue", "break", "pass", "return"),
				q(QuestionType.TRUE_FALSE, "Python dùng thụt lề để xác định khối lệnh.", "*Đúng", "Sai"));

		Section s3 = section(course, "Chương 3: Hàm và module", "Tự viết hàm, tham số mặc định, import module.", 3);
		Lesson l7 = lesson(s3, "Định nghĩa hàm với def", null, LessonType.VIDEO, 1, false);
		video(l7, "Video: Hàm trong Python", VIDEO_FLOWER, 1);
		Lesson l8 = lesson(s3, "Module và thư viện chuẩn", """
				Module là một file .py. Dùng import math hoặc from math import sqrt để dùng lại code.
				Một số module hay dùng: math, random, datetime, os, json.""", LessonType.ARTICLE, 2, false);
		document(l8, "Python Standard Library", "https://docs.python.org/3/library/index.html", "text/html", 1);
		image(l8, "Ảnh minh hoạ cấu trúc package", "python-package", 2);
		quiz(s3, "Kiểm tra cuối khoá Python", 3, "70.00", 3,
				q(QuestionType.SINGLE_CHOICE, "Từ khoá nào dùng để định nghĩa hàm?", "*def", "function", "fun", "lambda"),
				q(QuestionType.MULTIPLE_CHOICE, "Những module nào thuộc thư viện chuẩn Python? (chọn nhiều)",
						"*math", "*json", "numpy", "*datetime"),
				q(QuestionType.TRUE_FALSE, "Một hàm không có lệnh return sẽ trả về None.", "*Đúng", "Sai"),
				q(QuestionType.SINGLE_CHOICE, "def greet(name=\"bạn\") — name=\"bạn\" được gọi là gì?",
						"*Tham số mặc định", "Biến toàn cục", "Hằng số", "Decorator"));
		return course;
	}

	// ------------------------------------------------------------------ SQL

	private Course seedSql(String title) {
		Course course = course(title,
				"Thiết kế CSDL quan hệ và viết truy vấn SQL với PostgreSQL: SELECT, JOIN, GROUP BY, index và transaction.",
				"postgresql", category("Cơ sở dữ liệu", "Thiết kế và truy vấn cơ sở dữ liệu"), CourseStatus.PUBLISHED);

		Section s1 = section(course, "Chương 1: Truy vấn cơ bản", "SELECT, WHERE, ORDER BY, LIMIT.", 1);
		Lesson l1 = lesson(s1, "Giới thiệu CSDL quan hệ", null, LessonType.VIDEO, 1, true);
		video(l1, "Video: Bảng, hàng, cột và khoá chính", VIDEO_BUNNY, 1);
		image(l1, "Sơ đồ ERD hệ thống LMS", "sql-erd", 2);
		Lesson l2 = lesson(s1, "Câu lệnh SELECT và WHERE", """
				SELECT chọn cột, FROM chọn bảng, WHERE lọc hàng:

				SELECT title, status FROM courses WHERE status = 'PUBLISHED' ORDER BY title LIMIT 10;

				Dùng LIKE / ILIKE để tìm theo mẫu, IN để so với danh sách, BETWEEN cho khoảng giá trị
				và IS NULL để kiểm tra giá trị rỗng (không dùng = NULL).""", LessonType.ARTICLE, 2, false);
		document(l2, "PostgreSQL Tutorial: Querying a Table", "https://www.postgresql.org/docs/current/tutorial-select.html",
				"text/html", 1);
		quiz(s1, "Kiểm tra chương 1: SELECT", 3, "70.00", 3,
				q(QuestionType.SINGLE_CHOICE, "Mệnh đề nào dùng để lọc hàng?", "*WHERE", "ORDER BY", "GROUP BY", "LIMIT"),
				q(QuestionType.TRUE_FALSE, "Để kiểm tra giá trị rỗng ta viết WHERE col = NULL.", "Đúng", "*Sai"),
				q(QuestionType.SINGLE_CHOICE, "Trong PostgreSQL, toán tử nào so khớp mẫu không phân biệt hoa thường?",
						"*ILIKE", "LIKE", "SIMILAR", "MATCH"));

		Section s2 = section(course, "Chương 2: JOIN và gom nhóm", "Kết hợp nhiều bảng và thống kê.", 2);
		Lesson l3 = lesson(s2, "INNER JOIN và LEFT JOIN", null, LessonType.VIDEO, 1, false);
		video(l3, "Video: Các loại JOIN", VIDEO_SINTEL, 1);
		image(l3, "Biểu đồ Venn các loại JOIN", "sql-join-venn", 2);
		Lesson l4 = lesson(s2, "GROUP BY và HAVING", """
				GROUP BY gom các hàng có cùng giá trị, sau đó dùng hàm tổng hợp COUNT, SUM, AVG, MIN, MAX.
				HAVING lọc sau khi gom nhóm (WHERE lọc trước khi gom nhóm).

				SELECT category_id, COUNT(*) AS total
				FROM courses
				GROUP BY category_id
				HAVING COUNT(*) >= 2;""", LessonType.ARTICLE, 2, false);
		document(l4, "PostgreSQL: Aggregate Functions", "https://www.postgresql.org/docs/current/tutorial-agg.html",
				"text/html", 1);
		quiz(s2, "Kiểm tra chương 2: JOIN và GROUP BY", 3, "60.00", 5,
				q(QuestionType.SINGLE_CHOICE, "LEFT JOIN giữ lại những hàng nào?",
						"*Mọi hàng của bảng bên trái", "Chỉ hàng khớp ở cả hai bảng", "Mọi hàng của bảng bên phải",
						"Không giữ hàng nào"),
				q(QuestionType.SINGLE_CHOICE, "Mệnh đề nào lọc kết quả sau khi GROUP BY?", "*HAVING", "WHERE", "ON", "FILTER BY"),
				q(QuestionType.MULTIPLE_CHOICE, "Những hàm nào là hàm tổng hợp? (chọn nhiều)",
						"*COUNT", "*AVG", "UPPER", "*MAX"));

		Section s3 = section(course, "Chương 3: Index và transaction", "Tối ưu truy vấn và đảm bảo toàn vẹn dữ liệu.", 3);
		Lesson l5 = lesson(s3, "Index hoạt động thế nào?", """
				Index (mặc định B-tree) giúp tìm hàng nhanh hơn mà không phải quét toàn bảng.
				Đổi lại, mỗi lần INSERT / UPDATE phải cập nhật thêm index. Dùng EXPLAIN ANALYZE để xem
				truy vấn có dùng index hay không.""", LessonType.ARTICLE, 1, false);
		document(l5, "PostgreSQL: Indexes", "https://www.postgresql.org/docs/current/indexes.html", "text/html", 1);
		Lesson l6 = lesson(s3, "Transaction và ACID", null, LessonType.VIDEO, 2, false);
		video(l6, "Video: BEGIN, COMMIT, ROLLBACK", VIDEO_JELLYFISH, 1);
		quiz(s3, "Kiểm tra cuối khoá SQL", 3, "70.00", 3,
				q(QuestionType.MULTIPLE_CHOICE, "ACID gồm những tính chất nào? (chọn nhiều)",
						"*Atomicity", "*Consistency", "*Isolation", "*Durability", "Availability"),
				q(QuestionType.TRUE_FALSE, "Thêm index luôn làm mọi câu lệnh nhanh hơn, kể cả INSERT.", "Đúng", "*Sai"),
				q(QuestionType.SINGLE_CHOICE, "Lệnh nào huỷ toàn bộ thay đổi trong transaction hiện tại?",
						"*ROLLBACK", "COMMIT", "SAVEPOINT", "END"));
		return course;
	}

	// ------------------------------------------------------------------ JavaScript

	private Course seedJavaScript(String title) {
		Course course = course(title,
				"Nắm chắc JavaScript hiện đại trước khi học React / Vue: let/const, arrow function, destructuring, "
						+ "Promise và async/await.",
				"javascript", category("Lập trình Frontend", "Giao diện web: HTML, CSS, JavaScript, React…"),
				CourseStatus.PUBLISHED);

		Section s1 = section(course, "Chương 1: Cú pháp ES6", "Những thay đổi quan trọng nhất từ ES2015.", 1);
		Lesson l1 = lesson(s1, "let, const và phạm vi khối", """
				var có phạm vi hàm và bị hoisting, dễ gây lỗi. Từ ES6 nên dùng:
				- const: cho giá trị không gán lại (mặc định nên dùng)
				- let: khi cần gán lại giá trị

				Cả let và const đều có phạm vi khối { }.""", LessonType.ARTICLE, 1, true);
		document(l1, "MDN: let", "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Statements/let",
				"text/html", 1);
		Lesson l2 = lesson(s1, "Arrow function", null, LessonType.VIDEO, 2, true);
		video(l2, "Video: Arrow function và this", VIDEO_JELLYFISH, 1);
		Lesson l3 = lesson(s1, "Destructuring và spread", """
				const { title, status } = course;
				const [first, ...rest] = lessons;
				const updated = { ...course, status: 'PUBLISHED' };

				Spread (...) sao chép nông (shallow copy) object / mảng.""", LessonType.ARTICLE, 3, false);
		image(l3, "Cheat sheet destructuring", "js-destructuring", 1);
		quiz(s1, "Kiểm tra chương 1: ES6", 4, "70.00", 3,
				q(QuestionType.SINGLE_CHOICE, "Từ khoá nào nên dùng mặc định khi khai báo biến không gán lại?",
						"*const", "var", "let", "static"),
				q(QuestionType.TRUE_FALSE, "Spread operator tạo bản sao sâu (deep copy) của object.", "Đúng", "*Sai"),
				q(QuestionType.SINGLE_CHOICE, "const [a, ...b] = [1, 2, 3] thì b bằng gì?",
						"*[2, 3]", "[1, 2, 3]", "3", "[1]"));

		Section s2 = section(course, "Chương 2: Bất đồng bộ", "Callback, Promise, async/await và fetch API.", 2);
		Lesson l4 = lesson(s2, "Promise", null, LessonType.VIDEO, 1, false);
		video(l4, "Video: Promise từ A đến Z", VIDEO_SINTEL, 1);
		image(l4, "Sơ đồ trạng thái Promise", "js-promise-states", 2);
		Lesson l5 = lesson(s2, "async / await và fetch", """
				async function loadCourses() {
				  try {
				    const res = await fetch('/api/v1/courses');
				    if (!res.ok) throw new Error(res.status);
				    return await res.json();
				  } catch (err) {
				    console.error(err);
				  }
				}

				await chỉ dùng được trong hàm async (hoặc top-level module).""", LessonType.ARTICLE, 2, false);
		document(l5, "MDN: Using the Fetch API", "https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch",
				"text/html", 1);
		quiz(s2, "Kiểm tra cuối khoá JavaScript", 3, "70.00", 3,
				q(QuestionType.MULTIPLE_CHOICE, "Một Promise có thể ở những trạng thái nào? (chọn nhiều)",
						"*pending", "*fulfilled", "*rejected", "cancelled"),
				q(QuestionType.TRUE_FALSE, "fetch() tự ném lỗi khi server trả về 404.", "Đúng", "*Sai"),
				q(QuestionType.SINGLE_CHOICE, "Phương thức nào chạy song song nhiều Promise và chờ tất cả xong?",
						"*Promise.all", "Promise.race", "Promise.then", "Promise.wait"));
		return course;
	}

	// ------------------------------------------------------------------ Git

	private Course seedGit(String title) {
		Course course = course(title,
				"Quản lý mã nguồn với Git: commit, branch, merge, rebase, pull request và quy trình làm việc nhóm.",
				"git", category("DevOps", "Đóng gói, triển khai và vận hành ứng dụng"), CourseStatus.PUBLISHED);

		Section s1 = section(course, "Chương 1: Git cơ bản", "Repository, staging area và commit.", 1);
		Lesson l1 = lesson(s1, "Git là gì? Cài đặt và cấu hình", null, LessonType.VIDEO, 1, true);
		video(l1, "Video: Cài Git và git config", VIDEO_FLOWER, 1);
		document(l1, "Sách Pro Git (miễn phí)", "https://git-scm.com/book/en/v2", "text/html", 2);
		Lesson l2 = lesson(s1, "Ba vùng của Git", """
				Working directory → (git add) → Staging area → (git commit) → Repository.

				Lệnh hay dùng: git status, git add <file>, git commit -m "...", git log --oneline,
				git diff (chưa stage) và git diff --staged (đã stage).""", LessonType.ARTICLE, 2, false);
		image(l2, "Sơ đồ 3 vùng của Git", "git-three-areas", 1);
		quiz(s1, "Kiểm tra chương 1: Git cơ bản", 3, "70.00", 3,
				q(QuestionType.SINGLE_CHOICE, "Lệnh nào đưa thay đổi vào staging area?", "*git add", "git commit",
						"git push", "git stash"),
				q(QuestionType.SINGLE_CHOICE, "Lệnh nào xem lịch sử commit dạng ngắn gọn?",
						"*git log --oneline", "git status -s", "git show", "git diff"),
				q(QuestionType.TRUE_FALSE, "git commit gửi thay đổi lên GitHub.", "Đúng", "*Sai"));

		Section s2 = section(course, "Chương 2: Branch và làm việc nhóm", "Nhánh, merge, xung đột và pull request.", 2);
		Lesson l3 = lesson(s2, "Tạo nhánh và merge", null, LessonType.VIDEO, 1, false);
		video(l3, "Video: git switch -c và git merge", VIDEO_BUNNY, 1);
		Lesson l4 = lesson(s2, "Giải quyết xung đột (merge conflict)", """
				Xung đột xảy ra khi hai nhánh sửa cùng một đoạn code. Git đánh dấu bằng <<<<<<<, ======= và >>>>>>>.
				Sửa file giữ lại nội dung đúng, xoá các dấu đánh dấu, rồi git add và git commit để hoàn tất merge.""",
				LessonType.ARTICLE, 2, false);
		image(l4, "Ảnh: Xung đột trong VS Code", "git-conflict", 1);
		Lesson l5 = lesson(s2, "Pull request trên GitHub", null, LessonType.VIDEO, 3, false);
		video(l5, "Video: Quy trình pull request", VIDEO_SINTEL, 1);
		document(l5, "GitHub Docs: About pull requests",
				"https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/about-pull-requests",
				"text/html", 2);
		quiz(s2, "Kiểm tra cuối khoá Git", 4, "70.00", 3,
				q(QuestionType.SINGLE_CHOICE, "Lệnh nào tạo nhánh mới và chuyển sang nhánh đó?",
						"*git switch -c feature", "git branch -d feature", "git merge feature", "git checkout main"),
				q(QuestionType.MULTIPLE_CHOICE, "Những dấu nào Git dùng để đánh dấu xung đột? (chọn nhiều)",
						"*<<<<<<<", "*=======", "*>>>>>>>", "#######"),
				q(QuestionType.TRUE_FALSE, "Pull request là tính năng của GitHub, không phải lệnh có sẵn của Git.",
						"*Đúng", "Sai"));
		return course;
	}

	// ------------------------------------------------------------------ Pandas

	private Course seedPandas(String title) {
		Course course = course(title,
				"Đọc, làm sạch, biến đổi và trực quan hoá dữ liệu bảng bằng Pandas và Matplotlib.",
				"pandas", category("Khoa học dữ liệu", "Phân tích dữ liệu, thống kê và học máy"), CourseStatus.PUBLISHED);

		Section s1 = section(course, "Chương 1: DataFrame", "Series, DataFrame và đọc file CSV.", 1);
		Lesson l1 = lesson(s1, "Series và DataFrame", null, LessonType.VIDEO, 1, true);
		video(l1, "Video: Cấu trúc dữ liệu của Pandas", VIDEO_JELLYFISH, 1);
		image(l1, "Minh hoạ DataFrame", "pandas-dataframe", 2);
		Lesson l2 = lesson(s1, "Đọc và khám phá dữ liệu", """
				import pandas as pd
				df = pd.read_csv("students.csv")
				df.head()      # 5 dòng đầu
				df.info()      # kiểu dữ liệu, số giá trị khác null
				df.describe()  # thống kê mô tả các cột số
				df["gpa"].mean()""", LessonType.ARTICLE, 2, false);
		document(l2, "10 minutes to pandas", "https://pandas.pydata.org/docs/user_guide/10min.html", "text/html", 1);
		document(l2, "Dữ liệu mẫu (CSV)", "https://people.sc.fsu.edu/~jburkardt/data/csv/grades.csv", "text/csv", 2);
		quiz(s1, "Kiểm tra chương 1: DataFrame", 3, "70.00", 3,
				q(QuestionType.SINGLE_CHOICE, "Hàm nào đọc file CSV thành DataFrame?", "*pd.read_csv", "pd.open_csv",
						"pd.load", "pd.csv"),
				q(QuestionType.SINGLE_CHOICE, "df.head() mặc định trả về bao nhiêu dòng?", "*5", "10", "1", "Tất cả"),
				q(QuestionType.TRUE_FALSE, "Mỗi cột của DataFrame là một Series.", "*Đúng", "Sai"));

		Section s2 = section(course, "Chương 2: Làm sạch và trực quan hoá", "Xử lý dữ liệu thiếu, groupby và biểu đồ.", 2);
		Lesson l3 = lesson(s2, "Xử lý dữ liệu thiếu", """
				df.isna().sum() đếm số giá trị thiếu theo cột.
				df.dropna() bỏ dòng có giá trị thiếu; df.fillna(0) hoặc df["gpa"].fillna(df["gpa"].mean())
				để điền giá trị thay thế.""", LessonType.ARTICLE, 1, false);
		Lesson l4 = lesson(s2, "groupby và vẽ biểu đồ", null, LessonType.VIDEO, 2, false);
		video(l4, "Video: groupby và Matplotlib", VIDEO_BUNNY, 1);
		image(l4, "Biểu đồ cột điểm trung bình theo lớp", "pandas-bar-chart", 2);
		image(l4, "Biểu đồ đường số sinh viên theo năm", "pandas-line-chart", 3);
		quiz(s2, "Kiểm tra cuối khoá Pandas", 3, "60.00", null,
				q(QuestionType.SINGLE_CHOICE, "Cách nào đếm số giá trị thiếu theo từng cột?",
						"*df.isna().sum()", "df.count()", "df.null()", "df.missing()"),
				q(QuestionType.MULTIPLE_CHOICE, "Những cách nào xử lý được dữ liệu thiếu? (chọn nhiều)",
						"*dropna()", "*fillna()", "sort_values()", "*interpolate()"),
				q(QuestionType.TRUE_FALSE, "groupby() thường đi kèm một hàm tổng hợp như mean() hoặc sum().",
						"*Đúng", "Sai"));
		return course;
	}

	// ------------------------------------------------------------------ UI/UX (bản nháp)

	private Course seedUiUx(String title) {
		Course course = course(title,
				"Nguyên tắc thiết kế giao diện, màu sắc, typography và dựng wireframe / prototype trên Figma. "
						+ "(Khoá đang soạn — trạng thái bản nháp.)",
				"ui-ux", category("Thiết kế", "Thiết kế giao diện và trải nghiệm người dùng"), CourseStatus.DRAFT);

		Section s1 = section(course, "Chương 1: Nguyên tắc thiết kế", "Bố cục, khoảng trắng, màu và chữ.", 1);
		Lesson l1 = lesson(s1, "Màu sắc và độ tương phản", """
				Chọn 1 màu chủ đạo, 1–2 màu phụ và thang màu xám trung tính.
				Chữ thường trên nền phải đạt tỉ lệ tương phản tối thiểu 4.5:1 theo WCAG AA.""",
				LessonType.ARTICLE, 1, true);
		image(l1, "Bảng màu mẫu", "uiux-palette", 1);
		document(l1, "WCAG: Contrast (Minimum)",
				"https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html", "text/html", 2);
		Lesson l2 = lesson(s1, "Typography cơ bản", null, LessonType.VIDEO, 2, false);
		video(l2, "Video: Chọn font và thang cỡ chữ", VIDEO_BUNNY, 1);

		Section s2 = section(course, "Chương 2: Figma", "Frame, component và prototype.", 2);
		Lesson l3 = lesson(s2, "Dựng wireframe trang khoá học", null, LessonType.VIDEO, 1, false);
		video(l3, "Video: Wireframe trên Figma", VIDEO_SINTEL, 1);
		image(l3, "Wireframe trang danh sách khoá học", "uiux-wireframe", 2);
		image(l3, "Mockup trang chi tiết khoá học", "uiux-mockup", 3);
		quiz(s2, "Kiểm tra: Nguyên tắc thiết kế", 2, "70.00", 3,
				q(QuestionType.SINGLE_CHOICE, "Tỉ lệ tương phản tối thiểu cho chữ thường theo WCAG AA là bao nhiêu?",
						"*4.5:1", "2:1", "3:1", "7:1"),
				q(QuestionType.TRUE_FALSE, "Component trong Figma giúp tái sử dụng và cập nhật đồng loạt.",
						"*Đúng", "Sai"));
		return course;
	}

	// ------------------------------------------------------------------ helpers

	private Course course(String title, String description, String thumbnailSeed, Category category,
			CourseStatus status) {
		return courseRepository.save(Course.builder()
				.instructorId(INSTRUCTOR_1)
				.title(title)
				.description(description)
				.thumbnailUrl(picsum(thumbnailSeed, 600, 340))
				.category(category)
				.status(status)
				.build());
	}

	private Section section(Course course, String title, String description, int position) {
		return sectionRepository.save(Section.builder()
				.course(course).title(title).description(description).position(position).build());
	}

	private Lesson lesson(Section section, String title, String description, LessonType type, int position,
			boolean preview) {
		return lessonRepository.save(Lesson.builder()
				.section(section).title(title).description(description).type(type).position(position)
				.isPreview(preview).build());
	}

	private void video(Lesson lesson, String name, String url, int position) {
		material(lesson, name, MaterialType.VIDEO, url, 1_048_576L, "video/mp4", position);
	}

	private void image(Lesson lesson, String name, String seed, int position) {
		material(lesson, name, MaterialType.IMAGE, picsum(seed, 1200, 800), 180_000L, "image/jpeg", position);
	}

	private void document(Lesson lesson, String name, String url, String mime, int position) {
		material(lesson, name, MaterialType.DOCUMENT, url, null, mime, position);
	}

	private void material(Lesson lesson, String name, MaterialType type, String url, Long size, String mime,
			int position) {
		materialRepository.save(Material.builder()
				.lesson(lesson).name(name).type(type).url(url).fileSize(size).mimeType(mime).position(position)
				.build());
	}

	/** Ảnh mẫu cố định theo seed (cùng seed luôn ra cùng một ảnh). */
	private static String picsum(String seed, int width, int height) {
		return "https://picsum.photos/seed/lms-" + seed + "/" + width + "/" + height;
	}

	/** Tạo bài học QUIZ ở cuối chương cùng quiz và các câu hỏi. maxAttempts = null nghĩa là không giới hạn. */
	private void quiz(Section section, String title, int position, String passingScore, Integer maxAttempts,
			Question... questions) {
		Lesson quizLesson = lesson(section, title, "Bài kiểm tra " + questions.length + " câu hỏi.",
				LessonType.QUIZ, position, false);
		Quiz quiz = Quiz.builder()
				.lesson(quizLesson)
				.title(title)
				.description("Chọn đáp án đúng. Cần đạt " + new BigDecimal(passingScore).stripTrailingZeros().toPlainString()
						+ "% để qua bài.")
				.passingScore(new BigDecimal(passingScore))
				.maxAttempts(maxAttempts)
				.build();
		for (int i = 0; i < questions.length; i++) {
			questions[i].setPosition(i + 1);
			quiz.addQuestion(questions[i]);
		}
		quizRepository.save(quiz);
	}

	/** Câu hỏi; đáp án bắt đầu bằng {@code *} là đáp án đúng. */
	private static Question q(QuestionType type, String content, String... options) {
		Question question = Question.builder().content(content).type(type).position(0).build();
		for (int i = 0; i < options.length; i++) {
			boolean correct = options[i].startsWith("*");
			question.addOption(Option.builder()
					.content(correct ? options[i].substring(1) : options[i])
					.isCorrect(correct)
					.position(i + 1)
					.build());
		}
		return question;
	}

	/** Lấy danh mục mẫu theo tên, chưa có thì tạo mới (chạy lại nhiều lần không bị trùng). */
	private Category category(String name, String description) {
		Optional<Category> existing = categoryRepository.findByNameIgnoreCase(name);
		if (existing.isPresent()) {
			return existing.get();
		}
		return categoryRepository.save(Category.builder().name(name).description(description).build());
	}
}
