package com.andrei.demo.controller;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.ForgotPasswordRequest;
import com.andrei.demo.model.ResetPasswordRequest;
import com.andrei.demo.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@AllArgsConstructor
@CrossOrigin
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) throws ValidationException {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.ok(Map.of("message", "Reset code sent to " + request.email()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) throws ValidationException {
        passwordResetService.resetPassword(request.email(), request.code(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }
}