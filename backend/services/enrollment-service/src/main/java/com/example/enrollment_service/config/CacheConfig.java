package com.example.enrollment_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Configuration;

/**
 * A Redis outage must not break enrollment: cache failures are logged and the DB stays the source of truth.
 */
@Configuration
public class CacheConfig implements CachingConfigurer {

	private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

	@Override
	public CacheErrorHandler errorHandler() {
		return new SimpleCacheErrorHandler() {
			@Override
			public void handleCacheGetError(RuntimeException ex, Cache cache, Object key) {
				log.warn("Cache GET failed on {}: {}", cache.getName(), ex.getMessage());
			}

			@Override
			public void handleCachePutError(RuntimeException ex, Cache cache, Object key, Object value) {
				log.warn("Cache PUT failed on {}: {}", cache.getName(), ex.getMessage());
			}

			@Override
			public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
				log.warn("Cache EVICT failed on {}: {}", cache.getName(), ex.getMessage());
			}

			@Override
			public void handleCacheClearError(RuntimeException ex, Cache cache) {
				log.warn("Cache CLEAR failed on {}: {}", cache.getName(), ex.getMessage());
			}
		};
	}
}
