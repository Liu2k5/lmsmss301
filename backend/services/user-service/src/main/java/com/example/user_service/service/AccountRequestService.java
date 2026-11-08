package com.example.user_service.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.user_service.dto.AccountRequestResponse;
import com.example.user_service.entity.AccountRequest;
import com.example.user_service.entity.AccountRequestStatus;
import com.example.user_service.entity.AccountRequestType;
import com.example.user_service.entity.Role;
import com.example.user_service.entity.User;
import com.example.user_service.exception.ConflictException;
import com.example.user_service.exception.ResourceNotFoundException;
import com.example.user_service.repository.AccountRequestRepository;
import com.example.user_service.repository.UserRepository;

@Service
public class AccountRequestService {

    private final AccountRequestRepository requests;
    private final UserRepository users;
    private final KeycloakAdminService keycloak;

    public AccountRequestService(
            AccountRequestRepository requests,
            UserRepository users,
            KeycloakAdminService keycloak) {
        this.requests = requests;
        this.users = users;
        this.keycloak = keycloak;
    }

    @Transactional
    public AccountRequestResponse requestDeletion(String username) {
        User user = findActiveUser(username);
        return createRequest(user, AccountRequestType.DELETION);
    }

    @Transactional
    public AccountRequestResponse requestInstructorRole(String username) {
        User user = findActiveUser(username);
        if (user.getRole() != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Chỉ học sinh mới có thể gửi yêu cầu làm giảng viên");
        }
        return createRequest(user, AccountRequestType.INSTRUCTOR_ROLE);
    }

    @Transactional(readOnly = true)
    public java.util.List<AccountRequestResponse> myRequests(String username) {
        return requests.findAllByUsernameOrderByCreatedAtDesc(username).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AccountRequestResponse> listPending(Pageable pageable) {
        return requests.findAllByStatusOrderByCreatedAtDesc(AccountRequestStatus.PENDING, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public AccountRequestResponse review(long id, boolean approved, String note, String adminUsername) {
        AccountRequest request = requests.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));
        if (request.getStatus() != AccountRequestStatus.PENDING) {
            throw new ConflictException("Yêu cầu này đã được xử lý");
        }

        User user = users.findById(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if (approved) {
            if (request.getType() == AccountRequestType.DELETION) {
                keycloak.setUserEnabled(user.getUsername(), false);
                user.setEnabled(false);
                user.setDeleted(true);
            } else {
                if (user.getRole() != Role.STUDENT || user.isDeleted()) {
                    throw new ConflictException("Người dùng không còn đủ điều kiện để lên giảng viên");
                }
                keycloak.replaceRealmRole(user.getUsername(), Role.STUDENT, Role.INSTRUCTOR);
                user.setRole(Role.INSTRUCTOR);
            }
            users.save(user);
            request.setStatus(AccountRequestStatus.APPROVED);
        } else {
            request.setStatus(AccountRequestStatus.REJECTED);
        }

        request.setReviewedAt(java.time.Instant.now());
        request.setReviewedBy(adminUsername);
        request.setAdminNote(note == null || note.isBlank() ? null : note.trim());
        return toResponse(requests.save(request));
    }

    private AccountRequestResponse createRequest(User user, AccountRequestType type) {
        if (requests.existsByUsernameAndTypeAndStatus(
                user.getUsername(), type, AccountRequestStatus.PENDING)) {
            throw new ConflictException("Đã có yêu cầu cùng loại đang chờ xử lý");
        }

        AccountRequest request = new AccountRequest();
        request.setUsername(user.getUsername());
        request.setType(type);
        request.setStatus(AccountRequestStatus.PENDING);
        return toResponse(requests.save(request));
    }

    private User findActiveUser(String username) {
        User user = users.findById(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        if (!user.isEnabled() || user.isDeleted()) {
            throw new ConflictException("Tài khoản không hoạt động");
        }
        return user;
    }

    private AccountRequestResponse toResponse(AccountRequest request) {
        User user = users.findById(request.getUsername()).orElse(null);
        return new AccountRequestResponse(
                request.getId(),
                request.getUsername(),
                user == null ? null : user.getFullname(),
                user == null ? null : user.getEmail(),
                request.getType(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getReviewedAt(),
                request.getReviewedBy(),
                request.getAdminNote());
    }
}
