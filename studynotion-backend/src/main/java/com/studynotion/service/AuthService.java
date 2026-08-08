package com.studynotion.service;

import com.studynotion.dto.request.LoginRequest;
import com.studynotion.dto.request.SignupRequest;
import com.studynotion.dto.response.ApiResponse;
import com.studynotion.dto.response.AuthResponse;
import com.studynotion.dto.response.UserResponse;
import com.studynotion.entity.Otp;
import com.studynotion.entity.User;
import com.studynotion.exception.AppException;
import com.studynotion.repository.OtpRepository;
import com.studynotion.repository.UserRepository;
import com.studynotion.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @Transactional
    public ApiResponse<AuthResponse> signup(SignupRequest request) {
        // 1. Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException("Password and confirm password do not match", 400);
        }

        // 2. Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("User already exists with this email", 400);
        }

        // 3. Verify OTP
        Otp latestOtp = otpRepository
                .findTopByEmailOrderByCreatedAtDesc(request.getEmail())
                .orElseThrow(() -> new AppException("OTP not found. Please request a new OTP", 400));

        if (latestOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException("OTP has expired. Please request a new OTP", 400);
        }

        if (!latestOtp.getOtp().equals(request.getOtp())) {
            throw new AppException("Invalid OTP", 400);
        }

        // 4. Parse account type
        User.AccountType accountType;
        try {
            accountType = User.AccountType.valueOf(request.getAccountType());
        } catch (IllegalArgumentException e) {
            throw new AppException("Invalid account type. Must be Student or Instructor", 400);
        }

        // 5. Create user
        String avatarUrl = "https://api.dicebear.com/6.x/initials/svg?seed="
                + request.getFirstName() + " " + request.getLastName()
                + "&backgroundColor=00897b,00acc1,039be5,1e88e5,3949ab&fontWeight=600";

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .accountType(accountType)
                .contactNumber(request.getContactNumber())
                .image(avatarUrl)
                .active(true)
                .approved(true)
                .build();

        userRepository.save(user);

        // 6. Clean up OTP
        otpRepository.deleteByEmail(request.getEmail());

        // 7. Generate token
        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getAccountType().name());

        // 8. Send welcome email async
        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());

        log.info("New user registered: {} ({})", user.getEmail(), user.getAccountType());

        return ApiResponse.success("User registered successfully",
                AuthResponse.builder()
                        .token(token)
                        .user(UserResponse.from(user))
                        .build());
    }

    public ApiResponse<AuthResponse> login(LoginRequest request) {
        // 1. Find user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(
                        "User is not registered. Please sign up to continue", 401));

        // 2. Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException("Password is incorrect", 401);
        }

        // 3. Check if active
        if (!user.getActive()) {
            throw new AppException("Your account has been deactivated. Please contact support", 401);
        }

        // 4. Generate JWT
        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getAccountType().name());

        log.info("User logged in: {}", user.getEmail());

        return ApiResponse.success("Login successful",
                AuthResponse.builder()
                        .token(token)
                        .user(UserResponse.from(user))
                        .build());
    }

    @Transactional
    public ApiResponse<Void> sendOtp(String email) {
        // Check if user already registered
        if (userRepository.existsByEmail(email)) {
            throw new AppException("User is already registered with this email", 401);
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Delete old OTPs for this email
        otpRepository.deleteByEmail(email);

        // Save new OTP (expires in 5 minutes)
        Otp otpEntity = Otp.builder()
                .email(email)
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        otpRepository.save(otpEntity);

        // Send email
        emailService.sendOtpEmail(email, otp);

        log.info("OTP sent to: {}", email);

        return ApiResponse.success("OTP sent successfully to your email");
    }

    @Transactional
    public ApiResponse<Void> sendResetPasswordEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("No user found with this email", 404));

        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordExpires(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(email, token, user.getFirstName());

        return ApiResponse.success("Password reset link sent to your email");
    }

    @Transactional
    public ApiResponse<Void> resetPassword(String token, String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new AppException("Password and confirm password do not match", 400);
        }

        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new AppException("Invalid or expired reset token", 400));

        if (user.getResetPasswordExpires().isBefore(LocalDateTime.now())) {
            throw new AppException("Reset token has expired. Please request a new one", 400);
        }

        user.setPassword(passwordEncoder.encode(password));
        user.setResetPasswordToken(null);
        user.setResetPasswordExpires(null);
        userRepository.save(user);

        emailService.sendPasswordChangedEmail(user.getEmail(), user.getFirstName());

        return ApiResponse.success("Password reset successfully");
    }

    @Transactional
    public ApiResponse<Void> changePassword(Long userId, String oldPassword,
                                             String newPassword, String confirmNewPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", 404));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new AppException("Old password is incorrect", 401);
        }

        if (oldPassword.equals(newPassword)) {
            throw new AppException("New password cannot be same as old password", 400);
        }

        if (!newPassword.equals(confirmNewPassword)) {
            throw new AppException("New password and confirm password do not match", 400);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        emailService.sendPasswordChangedEmail(user.getEmail(), user.getFirstName());

        return ApiResponse.success("Password changed successfully");
    }
}
