package com.example.course_service.service.impl;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.course_service.config.MinioProperties;
import com.example.course_service.exception.StorageException;
import com.example.course_service.service.StorageService;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MinioStorageServiceImpl implements StorageService {

	private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

	private final MinioClient minioClient;
	private final MinioProperties properties;

	private volatile boolean bucketReady;

	@Override
	public StoredFile upload(String folder, MultipartFile file) {
		ensureBucket();
		// Kiểu file (MIME type): trình duyệt không gửi thì dùng kiểu mặc định "file nhị phân"
		String contentType;
		if (file.getContentType() == null) {
			contentType = DEFAULT_CONTENT_TYPE;
		}
		else {
			contentType = file.getContentType();
		}
		String objectKey = folder + "/" + UUID.randomUUID() + "-" + safeFileName(file.getOriginalFilename());

		try (InputStream input = file.getInputStream()) {
			minioClient.putObject(PutObjectArgs.builder()
					.bucket(properties.bucket())
					.object(objectKey)
					.stream(input, file.getSize(), -1)
					.contentType(contentType)
					.build());
		}
		catch (Exception ex) {
			throw new StorageException("Không upload được file lên MinIO. Kiểm tra MinIO đã chạy ở "
					+ properties.endpoint() + " chưa.", ex);
		}
		return new StoredFile(objectKey, file.getSize(), contentType);
	}

	@Override
	public String viewUrl(String objectKey) {
		try {
			return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
					.method(Method.GET)
					.bucket(properties.bucket())
					.object(objectKey)
					.expiry(properties.urlExpiryMinutes(), TimeUnit.MINUTES)
					.build());
		}
		catch (Exception ex) {
			throw new StorageException("Không tạo được link xem file " + objectKey, ex);
		}
	}

	@Override
	public void delete(String objectKey) {
		try {
			minioClient.removeObject(RemoveObjectArgs.builder()
					.bucket(properties.bucket())
					.object(objectKey)
					.build());
		}
		catch (Exception ex) {
			throw new StorageException("Không xoá được file " + objectKey + " trên MinIO", ex);
		}
	}

	/** Tạo bucket ở lần upload đầu tiên (không làm lúc khởi động để service chạy được khi MinIO chưa bật). */
	private void ensureBucket() {
		if (bucketReady) {
			return;
		}
		synchronized (this) {
			if (bucketReady) {
				return;
			}
			try {
				String bucket = properties.bucket();
				if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
					minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
				}
				bucketReady = true;
			}
			catch (Exception ex) {
				throw new StorageException("Không kết nối được MinIO ở " + properties.endpoint()
						+ ". Hãy chạy MinIO (docker compose up -d) rồi thử lại.", ex);
			}
		}
	}

	/** Giữ tên file dễ đọc nhưng chỉ gồm ký tự an toàn cho object key. */
	private static String safeFileName(String originalName) {
		// 1. Không có tên → dùng "file"
		String name;
		if (originalName == null || originalName.isBlank()) {
			name = "file";
		}
		else {
			name = originalName;
		}

		// 2. Bỏ phần thư mục nếu trình duyệt gửi cả đường dẫn: "C:\docs\bai1.pdf" → "bai1.pdf"
		name = name.replace('\\', '/');
		int lastSlash = name.lastIndexOf('/');
		name = name.substring(lastSlash + 1);

		// 3. Thay ký tự lạ (dấu tiếng Việt, khoảng trắng…) bằng "_": "Bài 1.pdf" → "B_i_1.pdf"
		name = name.replaceAll("[^A-Za-z0-9._-]", "_");

		// 4. Tên quá 100 ký tự → giữ 100 ký tự cuối (để không mất phần đuôi .pdf, .mp4)
		if (name.length() > 100) {
			name = name.substring(name.length() - 100);
		}
		return name;
	}
}
