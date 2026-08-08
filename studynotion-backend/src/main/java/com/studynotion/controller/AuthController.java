package com.studynotion.controller;

import com.studynotion.dto.request.LoginRequest;
import com.studynotion.dto.request.SignupRequest;
import com.studynotion.dto.response.ApiResponse;
import com.studynotion.dto.response.AuthResponse;
import com.studynotion.service.AuthService;
import com.studynotion.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        ApiResponse<AuthResponse> response = authService.signup(request);
        return ResponseEntity.ok(flattenAuthResponse(response));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        ApiResponse<AuthResponse> response = authService.login(request);
        return ResponseEntity.ok(flattenAuthResponse(response));
    }

    @PostMapping("/sendotp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.sendOtp(body.get("email")));
    }

    @PostMapping("/changepassword")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(authService.changePassword(
                userId,
                body.get("oldPassword"),
                body.get("newPassword"),
                body.get("confirmNewPassword")
        ));
    }

    @PostMapping("/reset-password-token")
    public ResponseEntity<?> sendResetPasswordEmail(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.sendResetPasswordEmail(body.get("email")));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.resetPassword(
                body.get("token"),
                body.get("password"),
                body.get("confirmPassword")
        ));
    }

    private Map<String, Object> flattenAuthResponse(ApiResponse<AuthResponse> apiResponse) {
        Map<String, Object> flat = new HashMap<>();
        flat.put("success", apiResponse.isSuccess());
        flat.put("message", apiResponse.getMessage());
        if (apiResponse.getData() != null) {
            flat.put("token", apiResponse.getData().getToken());
            flat.put("user", apiResponse.getData().getUser());
        }
        return flat;
    }
}

