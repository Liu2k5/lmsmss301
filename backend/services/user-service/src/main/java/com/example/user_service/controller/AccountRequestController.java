package com.example.user_service.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.user_service.dto.AccountRequestDecision;
import com.example.user_service.dto.AccountRequestResponse;
import com.example.user_service.service.AccountRequestService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class AccountRequestController {

    private final AccountRequestService requests;

    public AccountRequestController(AccountRequestService requests) {
        this.requests = requests;
    }

    @GetMapping("/me/requests")
    public java.util.List<AccountRequestResponse> myRequests(@AuthenticationPrincipal Jwt jwt) {
        return requests.myRequests(usernameFrom(jwt));
    }

    @PostMapping("/me/requests/deletion")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountRequestResponse requestDeletion(@AuthenticationPrincipal Jwt jwt) {
        return requests.requestDeletion(usernameFrom(jwt));
    }

    @PostMapping("/me/requests/instructor-role")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountRequestResponse requestInstructorRole(@AuthenticationPrincipal Jwt jwt) {
        return requests.requestInstructorRole(usernameFrom(jwt));
    }

    @GetMapping("/admin/requests")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public Page<AccountRequestResponse> pendingRequests(Pageable pageable) {
        return requests.listPending(pageable);
    }

    @PatchMapping("/admin/requests/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public AccountRequestResponse review(
            @PathVariable long id,
            @Valid @RequestBody AccountRequestDecision decision,
            @AuthenticationPrincipal Jwt jwt) {
        return requests.review(id, decision.approved(), decision.note(), usernameFrom(jwt));
    }

    private String usernameFrom(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "JWT không có preferred_username");
        }
        return username;
    }
}
