package com.example.user_service.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.user_service.dto.ChangePasswordRequest;
import com.example.user_service.dto.ResetPasswordRequest;
import com.example.user_service.entity.PasswordResetCode;
import com.example.user_service.entity.User;
import com.example.user_service.repository.PasswordResetCodeRepository;
import com.example.user_service.repository.UserRepository;

@Service
public class PasswordResetService {

    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration SEND_WINDOW = Duration.ofHours(1);
    private static final int MAX_SENDS_PER_WINDOW = 3;
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final PasswordResetCodeRepository resetCodes;
    private final KeycloakAdminService keycloak;
    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${app.mail.from:${spring.mail.username:}}")
    private String fromAddress;

    public PasswordResetService(
            UserRepository users,
            PasswordResetCodeRepository resetCodes,
            KeycloakAdminService keycloak,
            JavaMailSender mailSender) {
        this.users = users;
        this.resetCodes = resetCodes;
        this.keycloak = keycloak;
        this.mailSender = mailSender;
    }

    @Transactional
    public void requestCode(String email) {
        users.findByEmailIgnoreCase(email.trim()).filter(user -> user.isEnabled() && !user.isDeleted())
                .ifPresent(user -> sendCode(user));
    }

    private void sendCode(User user) {
        Instant now = Instant.now();
        PasswordResetCode existing = resetCodes.findById(user.getUsername()).orElse(null);
        if (existing != null) {
            if (existing.getLastSentAt().isAfter(now.minus(RESEND_COOLDOWN))) {
                return;
            }
            if (existing.getWindowStartedAt().isBefore(now.minus(SEND_WINDOW))) {
                existing.setWindowStartedAt(now);
                existing.setSentCount(0);
            }
            if (existing.getSentCount() >= MAX_SENDS_PER_WINDOW) {
                return;
            }
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        PasswordResetCode resetCode = existing == null ? new PasswordResetCode() : existing;
        resetCode.setUsername(user.getUsername());
        resetCode.setCodeHash(passwordEncoder.encode(code));
        resetCode.setCreatedAt(now);
        resetCode.setLastSentAt(now);
        if (existing == null) {
            resetCode.setWindowStartedAt(now);
            resetCode.setSentCount(0);
        }
        resetCode.setSentCount(resetCode.getSentCount() + 1);
        resetCode.setExpiresAt(now.plus(CODE_TTL));
        resetCode.setAttempts(0);
        resetCodes.save(resetCode);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank()) {
                message.setFrom(fromAddress);
            }
            message.setTo(user.getEmail());
            message.setSubject("Mã xác nhận đặt lại mật khẩu LearnHub");
            message.setText("Mã xác nhận của bạn là " + code
                    + ". Mã có hiệu lực trong 10 phút và chỉ sử dụng được một lần.");
            mailSender.send(message);
        } catch (MailException ex) {
            resetCodes.deleteById(user.getUsername());
            throw ex;
        }
    }

    @Transactional
    public boolean resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu xác nhận không khớp");
        }

        User user = users.findByEmailIgnoreCase(request.email().trim()).orElse(null);
        if (user == null || !user.isEnabled() || user.isDeleted()) {
            return false;
        }

        PasswordResetCode resetCode = resetCodes.findById(user.getUsername()).orElse(null);
        if (resetCode == null || resetCode.getExpiresAt().isBefore(Instant.now())
                || resetCode.getAttempts() >= MAX_ATTEMPTS) {
            return false;
        }

        if (!passwordEncoder.matches(request.code(), resetCode.getCodeHash())) {
            resetCode.setAttempts(resetCode.getAttempts() + 1);
            resetCodes.save(resetCode);
            return false;
        }

        keycloak.resetPassword(user.getUsername(), request.newPassword());
        resetCodes.delete(resetCode);
        return true;
    }

    public void changePassword(
            String accessToken, ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu xác nhận không khớp");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        keycloak.changePassword(request.currentPassword(), request.newPassword(), accessToken);
    }
}
