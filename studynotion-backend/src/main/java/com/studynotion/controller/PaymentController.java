package com.studynotion.controller;

import com.studynotion.exception.AppException;
import com.studynotion.service.PaymentService;
import com.studynotion.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/capturePayment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> capturePayment(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(paymentService.capturePayment(
                extractCourseIds(body), SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/verifyPayment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(paymentService.verifyPaymentAndEnroll(
                (String) body.get("razorpay_order_id"),
                (String) body.get("razorpay_payment_id"),
                (String) body.get("razorpay_signature"),
                extractCourseIds(body),
                SecurityUtils.getCurrentUserId()));
    }

    /** {@code courses} may be a JSON array of numbers/strings, or a single value. */
    @SuppressWarnings("unchecked")
    private static List<Long> extractCourseIds(Map<String, Object> body) {
        Object raw = body.get("courses");
        if (raw == null) raw = body.get("courseId");
        List<Long> ids = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object o : list) ids.add(toLong(o));
        } else if (raw != null) {
            ids.add(toLong(raw));
        }
        if (ids.isEmpty()) {
            throw new AppException("Please provide at least one course to purchase", 400);
        }
        return ids;
    }

    private static Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof Map<?, ?> m) { // frontend sometimes sends whole course objects
            Object id = m.containsKey("id") ? m.get("id") : m.get("_id");
            return toLong(id);
        }
        return Long.parseLong(value.toString().trim());
    }
}
