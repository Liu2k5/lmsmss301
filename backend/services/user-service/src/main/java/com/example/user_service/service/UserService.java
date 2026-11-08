package com.example.user_service.service;

import com.example.user_service.dto.*;
import com.example.user_service.entity.*;
import com.example.user_service.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.user_service.exception.ConflictException;
import com.example.user_service.exception.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository users;
    private final UniversityRepository universities;
    private final KeycloakAdminService keycloak;

    // 1. Register
    @Transactional
    public UserResponse register(RegisterRequest r) {
        return createUser(r, false);
    }

    @Transactional
    public AdminUserResponse createUserByAdmin(RegisterRequest r) {
        return toAdminResponse(createUserEntity(r, true));
    }

    private UserResponse createUser(RegisterRequest r, boolean administratorAllowed) {
        return toResponse(createUserEntity(r, administratorAllowed));
    }

    private User createUserEntity(RegisterRequest r, boolean administratorAllowed) {
        if (!administratorAllowed && r.role() == Role.ADMINISTRATOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot self-register as admin");
        }
        if (users.existsById(r.username())
                || users.existsByEmail(r.email())) {
            throw new ConflictException("Username or email already exists");
        }

        University university = null;
        if (r.universityId() != null) {
            university = universities.findById(r.universityId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "University not found"));
        }

        keycloak.createUser(r);

        User u = new User();

        u.setUsername(r.username());
        u.setFullname(r.fullname());
        u.setEmail(r.email());
        u.setPhone(r.phone());
        u.setRole(r.role());

        u.setUniversity(university);

        return users.save(u);
    }

    // 2. Get current user
    @Transactional(readOnly = true)
    public UserResponse getMe(String username) {

        return toResponse(
            users.findById(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
        );
    }

    // 3. Update profile
    @Transactional
    public UserResponse updateMe(
            String username,
            UpdateProfileRequest r) {

        User u = users.findById(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        u.setFullname(r.fullname());
        u.setPhone(r.phone());

        u.setUniversity(r.universityId() == null ? null : universities.findById(r.universityId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "University not found")));

        return toResponse(u);
    }

    // Convert Entity to DTO
    private UserResponse toResponse(User u) {

        var uni = u.getUniversity();

        return new UserResponse(
            u.getUsername(),
            u.getFullname(),
            u.getEmail(),
            u.getPhone(),
            u.getRole(),
            uni == null ? null : uni.getId(),
            uni == null ? null : uni.getName()
        );
    }

    public Page<UserResponse> list(Pageable pageable) {
        return users.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listAdminUsers(Pageable pageable) {
        return users.findAll(pageable).map(this::toAdminResponse);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getAdminUser(String username) {
        return users.findById(username)
                .map(this::toAdminResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public AdminUserResponse updateUser(String username, AdminUserUpdateRequest request) {
        User user = users.findById(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setUniversity(request.universityId() == null ? null : universities.findById(request.universityId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "University not found")));

        keycloak.updateUser(username, request, user.getRole());
        user.setFullname(request.fullname());
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.setEnabled(request.enabled());
        return toAdminResponse(users.save(user));
    }

    @Transactional
    public AdminUserResponse setUserEnabled(String username, boolean enabled) {
        User user = users.findById(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        keycloak.setUserEnabled(username, enabled);
        user.setEnabled(enabled);
        return toAdminResponse(users.save(user));
    }

    private AdminUserResponse toAdminResponse(User u) {
        var uni = u.getUniversity();
        return new AdminUserResponse(
                u.getUsername(),
                u.getFullname(),
                u.getEmail(),
                u.getPhone(),
                u.getRole(),
                uni == null ? null : uni.getId(),
                uni == null ? null : uni.getName(),
                u.isEnabled());
    }
}