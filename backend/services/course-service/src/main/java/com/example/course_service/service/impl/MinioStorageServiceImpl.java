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
		String contentType = file.getContentType() == null ? DEFAULT_CONTENT_TYPE : file.getContentType();
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
		String name = originalName == null || originalName.isBlank() ? "file" : originalName;
		name = name.replace('\\', '/');
		name = name.substring(name.lastIndexOf('/') + 1);
		name = name.replaceAll("[^A-Za-z0-9._-]", "_");
		return name.length() > 100 ? name.substring(name.length() - 100) : name;
	}
}
