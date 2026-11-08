package com.example.review_rating_service.security;

import com.example.review_rating_service.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<UserContext> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserContext userContext) {
            return Optional.of(userContext);
        }
        return Optional.empty();
    }

    public static UserContext getRequiredCurrentUser() {
        return getCurrentUser()
                .orElseThrow(() -> new UnauthorizedException("Yêu cầu đăng nhập để thực hiện hành động này"));
    }

    public static UUID getCurrentUserId() {
        return getRequiredCurrentUser().getUserId();
    }

    public static boolean isAdmin() {
        return getCurrentUser().map(UserContext::isAdmin).orElse(false);
    }
}
