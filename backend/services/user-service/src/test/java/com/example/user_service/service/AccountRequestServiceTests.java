package com.example.user_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.user_service.entity.AccountRequest;
import com.example.user_service.entity.AccountRequestStatus;
import com.example.user_service.entity.AccountRequestType;
import com.example.user_service.entity.Role;
import com.example.user_service.entity.User;
import com.example.user_service.repository.AccountRequestRepository;
import com.example.user_service.repository.UserRepository;

class AccountRequestServiceTests {

    @Mock
    private AccountRequestRepository requests;

    @Mock
    private UserRepository users;

    @Mock
    private KeycloakAdminService keycloak;

    private AccountRequestService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AccountRequestService(requests, users, keycloak);
        when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void approvingDeletionSoftDeletesAndDisablesUser() {
        User user = user(Role.STUDENT);
        AccountRequest request = request(AccountRequestType.DELETION, user.getUsername());
        when(requests.findById(12L)).thenReturn(Optional.of(request));
        when(users.findById(user.getUsername())).thenReturn(Optional.of(user));

        var result = service.review(12L, true, "Confirmed", "admin");

        assertEquals(AccountRequestStatus.APPROVED, result.status());
        assertTrue(user.isDeleted());
        assertFalse(user.isEnabled());
        verify(keycloak).setUserEnabled(user.getUsername(), false);
    }

    @Test
    void approvingInstructorRequestChangesDatabaseAndKeycloakRole() {
        User user = user(Role.STUDENT);
        AccountRequest request = request(AccountRequestType.INSTRUCTOR_ROLE, user.getUsername());
        when(requests.findById(13L)).thenReturn(Optional.of(request));
        when(users.findById(user.getUsername())).thenReturn(Optional.of(user));

        var result = service.review(13L, true, null, "admin");

        assertEquals(AccountRequestStatus.APPROVED, result.status());
        assertEquals(Role.INSTRUCTOR, user.getRole());
        verify(keycloak).replaceRealmRole(user.getUsername(), Role.STUDENT, Role.INSTRUCTOR);
    }

    private User user(Role role) {
        User user = new User();
        user.setUsername("learner");
        user.setFullname("Test Learner");
        user.setEmail("learner@example.test");
        user.setRole(role);
        return user;
    }

    private AccountRequest request(AccountRequestType type, String username) {
        AccountRequest request = new AccountRequest();
        request.setId(1L);
        request.setUsername(username);
        request.setType(type);
        request.setStatus(AccountRequestStatus.PENDING);
        return request;
    }
}
