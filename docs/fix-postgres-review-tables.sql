-- ==============================================================================
-- LEARN HUB LMS - RESET SCHEMA CHO REVIEW & RATING SERVICE (POSTGRESQL)
-- ==============================================================================
-- Mục đích: Khắc phục lỗi "operator does not exist: integer = uuid" khi chuyển đổi
-- sang UUID. Do Hibernate ddl-auto=update không tự đổi kiểu dữ liệu của cột cũ từ
-- integer sang uuid, ta cần DROP các bảng cũ để Hibernate tự động sinh lại bảng mới.
--
-- Cách thực hiện:
-- 1. Chạy các câu lệnh DROP bên dưới trong database của bạn (Neon DB console hoặc DBeaver/pgAdmin).
-- 2. Khởi động lại service, Hibernate sẽ tự động CREATE lại bảng với kiểu UUID chuẩn và nạp seed data.
-- ==============================================================================

DROP TABLE IF EXISTS review_helpful_votes CASCADE;
DROP TABLE IF EXISTS review_replies CASCADE;
DROP TABLE IF EXISTS review_reports CASCADE;
DROP TABLE IF EXISTS review_permissions CASCADE;
DROP TABLE IF EXISTS course_rating_summaries CASCADE;
DROP TABLE IF EXISTS reviews CASCADE;
