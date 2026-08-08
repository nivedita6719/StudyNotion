package com.studynotion.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.studynotion.dto.response.ApiResponse;
import com.studynotion.entity.*;
import com.studynotion.exception.AppException;
import com.studynotion.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseProgressRepository courseProgressRepository;
    private final EmailService emailService;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    public ApiResponse<Map<String, Object>> capturePayment(List<Long> courseIds, Long userId) {
        if (courseIds == null || courseIds.isEmpty()) {
            throw new AppException("Please provide valid course IDs", 400);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", 404));

        double totalAmount = 0;
        List<Course> courses = new ArrayList<>();

        for (Long courseId : courseIds) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new AppException("Course not found: " + courseId, 404));

            // Check if already enrolled
            if (course.getStudentsEnrolled().contains(user)) {
                throw new AppException("Already enrolled in course: " + course.getCourseName(), 400);
            }

            courses.add(course);
            totalAmount += course.getPrice();
        }

        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int)(totalAmount * 100)); // paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "order_" + System.currentTimeMillis());

            Order order = client.orders.create(orderRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("currency", order.get("currency"));
            response.put("amount", order.get("amount"));

            log.info("Payment order created: {} for user: {}", order.get("id"), user.getEmail());
            return ApiResponse.success("Payment initiated", response);

        } catch (Exception e) {
            log.error("Razorpay error: {}", e.getMessage());
            throw new AppException("Payment initiation failed: " + e.getMessage(), 500);
        }
    }

    @Transactional
    public ApiResponse<Void> verifyPaymentAndEnroll(
            String razorpayOrderId, String razorpayPaymentId,
            String razorpaySignature, List<Long> courseIds, Long userId) {

        // 1. Verify signature
        String generatedSignature = generateHmacSHA256(razorpayOrderId + "|" + razorpayPaymentId);
        if (!generatedSignature.equals(razorpaySignature)) {
            throw new AppException("Payment verification failed. Invalid signature.", 400);
        }

        // 2. Enroll user in all courses
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", 404));

        double totalAmount = 0;
        List<String> enrolledCourseNames = new ArrayList<>();

        for (Long courseId : courseIds) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new AppException("Course not found", 404));

            // Add student to course
            if (!course.getStudentsEnrolled().contains(user)) {
                course.getStudentsEnrolled().add(user);
                courseRepository.save(course);
            }

            // Add course to user
            if (!user.getCourses().contains(course)) {
                user.getCourses().add(course);
            }

            // Create course progress
            courseProgressRepository.findByUserIdAndCourseId(userId, courseId)
                    .orElseGet(() -> {
                        CourseProgress progress = CourseProgress.builder()
                                .user(user)
                                .course(course)
                                .build();
                        return courseProgressRepository.save(progress);
                    });

            totalAmount += course.getPrice();
            enrolledCourseNames.add(course.getCourseName());

            // Send enrollment email
            emailService.sendCourseEnrollmentEmail(
                    user.getEmail(), user.getFirstName(), course.getCourseName());
        }

        userRepository.save(user);

        // Send payment success email
        emailService.sendPaymentSuccessEmail(
                user.getEmail(), user.getFirstName(), razorpayOrderId, totalAmount);

        log.info("User {} enrolled in courses: {}", user.getEmail(), enrolledCourseNames);
        return ApiResponse.success("Payment verified and enrollment successful");
    }

    private String generateHmacSHA256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    razorpayKeySecret.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new AppException("Signature generation failed", 500);
        }
    }
}
