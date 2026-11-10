package com.example.review_rating_service.client;

import com.example.review_rating_service.security.SecurityUtils;
import com.example.review_rating_service.security.UserContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Slf4j
@Component
public class FeignHeaderInterceptor implements RequestInterceptor {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_USER_EMAIL = "X-User-Email";

    @Override
    public void apply(RequestTemplate template) {
        // 1. Try to propagate from Spring Security UserContext
        Optional<UserContext> currentUser = SecurityUtils.getCurrentUser();
        if (currentUser.isPresent()) {
            UserContext user = currentUser.get();
            if (user.getUserId() != null && !template.headers().containsKey(HEADER_USER_ID)) {
                template.header(HEADER_USER_ID, user.getUserId().toString());
            }
            if (user.getRole() != null && !template.headers().containsKey(HEADER_USER_ROLE)) {
                template.header(HEADER_USER_ROLE, user.getRole());
            }
            if (user.getEmail() != null && !template.headers().containsKey(HEADER_USER_EMAIL)) {
                template.header(HEADER_USER_EMAIL, user.getEmail());
            }
            return;
        }

        // 2. Fallback to HttpServletRequest attributes if available
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String userId = request.getHeader(HEADER_USER_ID);
            if (userId != null && !template.headers().containsKey(HEADER_USER_ID)) {
                template.header(HEADER_USER_ID, userId);
            }
            String userRole = request.getHeader(HEADER_USER_ROLE);
            if (userRole != null && !template.headers().containsKey(HEADER_USER_ROLE)) {
                template.header(HEADER_USER_ROLE, userRole);
            }
            String userEmail = request.getHeader(HEADER_USER_EMAIL);
            if (userEmail != null && !template.headers().containsKey(HEADER_USER_EMAIL)) {
                template.header(HEADER_USER_EMAIL, userEmail);
            }
        }
    }
}
