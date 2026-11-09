package com.example.user_service.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.user_service.dto.ForgotPasswordRequest;
import com.example.user_service.dto.ResetPasswordRequest;
import com.example.user_service.service.PasswordResetService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class PasswordController {

    private final PasswordResetService passwords;

    public PasswordController(PasswordResetService passwords) {
        this.passwords = passwords;
    }

    @PostMapping("/password/forgot")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> requestPasswordReset(@Valid @RequestBody ForgotPasswordRequest request) {
        passwords.requestCode(request.email());
        return Map.of("message", "Nếu email có tài khoản hợp lệ, mã xác nhận sẽ được gửi đến hộp thư.");
    }

    @PostMapping("/password/reset")
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        if (!passwords.resetPassword(request)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Mã không hợp lệ, đã hết hạn hoặc đã vượt số lần thử");
        }
        return Map.of("message", "Đặt lại mật khẩu thành công.");
    }

}
