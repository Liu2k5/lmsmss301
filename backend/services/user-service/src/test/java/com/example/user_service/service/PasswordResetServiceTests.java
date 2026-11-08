package com.example.user_service.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.example.user_service.dto.ResetPasswordRequest;
import com.example.user_service.entity.PasswordResetCode;
import com.example.user_service.entity.Role;
import com.example.user_service.entity.User;
import com.example.user_service.repository.PasswordResetCodeRepository;
import com.example.user_service.repository.UserRepository;

class PasswordResetServiceTests {

    @Mock
    private UserRepository users;

    @Mock
    private PasswordResetCodeRepository resetCodes;

    @Mock
    private KeycloakAdminService keycloak;

    @Mock
    private JavaMailSender mailSender;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PasswordResetService(users, resetCodes, keycloak, mailSender);
    }

    @Test
    void emailedCodeCanOnlyResetPasswordOnce() {
        User user = new User();
        user.setUsername("learner");
        user.setEmail("learner@example.test");
        user.setRole(Role.STUDENT);
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));

        AtomicReference<PasswordResetCode> savedCode = new AtomicReference<>();
        when(resetCodes.findById(user.getUsername()))
                .thenAnswer(invocation -> Optional.ofNullable(savedCode.get()));
        when(resetCodes.save(any())).thenAnswer(invocation -> {
            PasswordResetCode code = invocation.getArgument(0);
            savedCode.set(code);
            return code;
        });

        service.requestCode(user.getEmail());

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\b(\\d{6})\\b").matcher(message.getValue().getText());
        org.junit.jupiter.api.Assertions.assertTrue(matcher.find());
        String code = matcher.group(1);

        ResetPasswordRequest request = new ResetPasswordRequest(
                user.getEmail(), code, "new-password-123", "new-password-123");
        assertTrue(service.resetPassword(request));
        verify(keycloak).resetPassword(user.getUsername(), request.newPassword());
        verify(resetCodes).delete(savedCode.get());
    }
}
