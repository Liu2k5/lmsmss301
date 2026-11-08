package com.example.user_service.controller;

import com.example.user_service.dto.*;
import com.example.user_service.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    // Register
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(
            @Valid @RequestBody RegisterRequest r) {

        return service.register(r);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return service.getMe(usernameFrom(jwt));
    }

    @PutMapping("/me")
    public UserResponse update(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest r) {
        return service.updateMe(usernameFrom(jwt), r);
    }

    // Service khác (Enrollment, Review...) cần tra thông tin người dùng
    @GetMapping("/{username}")
    public UserResponse byUsername(@PathVariable String username) {
        return service.getMe(username);
    }

    @GetMapping({"", "/admin"})
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public Page<AdminUserResponse> list(Pageable pageable) {
        return service.listAdminUsers(pageable);
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserResponse createUser(@Valid @RequestBody RegisterRequest request) {
        return service.createUserByAdmin(request);
    }

    @GetMapping("/admin/{username}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public AdminUserResponse adminUser(@PathVariable String username) {
        return service.getAdminUser(username);
    }

    @PutMapping({"/{username}", "/admin/{username}"})
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public AdminUserResponse updateUser(@PathVariable String username,
            @Valid @RequestBody AdminUserUpdateRequest request) {
        return service.updateUser(username, request);
    }

    @PatchMapping("/admin/{username}/enabled")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public AdminUserResponse setUserEnabled(@PathVariable String username,
            @Valid @RequestBody AdminUserEnabledRequest request) {
        return service.setUserEnabled(username, request.enabled());
    }

    private String usernameFrom(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "JWT does not contain preferred_username");
        }
        return username;
    }
}