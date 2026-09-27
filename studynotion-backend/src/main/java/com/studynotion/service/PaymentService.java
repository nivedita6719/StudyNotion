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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

    /**
     * Creates a Razorpay order for the given courses. Read-only transaction so the
     * enrolled-students check does not trip a LazyInitializationException.
     */
    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> capturePayment(List<Long> courseIds, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", 404));

        double totalAmount = 0;
        for (Long courseId : courseIds) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new AppException("Course not found: " + courseId, 404));

            if (course.getStatus() != Course.CourseStatus.Published) {
                throw new AppException("Course is not available for purchase: " + course.getCourseName(), 400);
            }
            if (course.getInstructor() != null && course.getInstructor().getId().equals(userId)) {
                throw new AppException("You cannot buy your own course", 400);
            }
            if (user.getCourses().contains(course)) {
                throw new AppException("Already enrolled in course: " + course.getCourseName(), 400);
            }
            totalAmount += course.getPrice() != null ? course.getPrice() : 0;
        }

        if (totalAmount <= 0) {
            throw new AppException("Invalid order amount", 400);
        }

        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", Math.round(totalAmount * 100)); // paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "receipt_" + System.currentTimeMillis());

            Order order = client.orders.create(orderRequest);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("orderId", order.get("id"));
            response.put("id", order.get("id"));
            response.put("currency", order.get("currency"));
            response.put("amount", order.get("amount"));
            response.put("keyId", razorpayKeyId);

            log.info("Payment order {} created for {} ({} paise)", order.get("id"), user.getEmail(), orderRequest.get("amount"));
            return ApiResponse.success("Payment initiated", response);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Razorpay order creation failed", e);
            throw new AppException("Payment initiation failed. Please try again.", 502);
        }
    }

    @Transactional
    public ApiResponse<Void> verifyPaymentAndEnroll(
            String razorpayOrderId, String razorpayPaymentId,
            String razorpaySignature, List<Long> courseIds, Long userId) {

        if (razorpayOrderId == null || razorpayPaymentId == null || razorpaySignature == null) {
            throw new AppException("Incomplete payment details", 400);
        }

        String expected = hmacSha256(razorpayOrderId + "|" + razorpayPaymentId, razorpayKeySecret);
        if (!constantTimeEquals(expected, razorpaySignature)) {
            log.warn("Payment signature mismatch for order {}", razorpayOrderId);
            throw new AppException("Payment verification failed. Invalid signature.", 400);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", 404));

        double totalAmount = 0;
        List<String> enrolledCourseNames = new ArrayList<>();

        for (Long courseId : courseIds) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new AppException("Course not found", 404));

            if (!user.getCourses().contains(course)) {
                user.getCourses().add(course);              // owning side of the M:N
                course.getStudentsEnrolled().add(user);      // keep in-memory graph consistent
            }

            courseProgressRepository.findByUserIdAndCourseId(userId, courseId)
                    .orElseGet(() -> courseProgressRepository.save(
                            CourseProgress.builder().user(user).course(course).build()));

            totalAmount += course.getPrice() != null ? course.getPrice() : 0;
            enrolledCourseNames.add(course.getCourseName());

            emailService.sendCourseEnrollmentEmail(
                    user.getEmail(), user.getFirstName(), course.getCourseName());
        }

        userRepository.save(user);
        emailService.sendPaymentSuccessEmail(
                user.getEmail(), user.getFirstName(), razorpayOrderId, totalAmount);

        log.info("User {} enrolled in {}", user.getEmail(), enrolledCourseNames);
        return ApiResponse.success("Payment verified and enrollment successful");
    }

    // ---------------------------------------------------------------------

    private static String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new AppException("Signature generation failed", 500);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
