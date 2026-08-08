package com.studynotion.controller;

import com.studynotion.service.PaymentService;
import com.studynotion.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/capturePayment")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> capturePayment(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        @SuppressWarnings("unchecked")
        List<Long> courseIds = (List<Long>) body.get("courses");
        return ResponseEntity.ok(paymentService.capturePayment(courseIds, userId));
    }

    @PostMapping("/verifyPayment")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        @SuppressWarnings("unchecked")
        List<Long> courseIds = (List<Long>) body.get("courses");
        return ResponseEntity.ok(paymentService.verifyPaymentAndEnroll(
                (String) body.get("razorpay_order_id"),
                (String) body.get("razorpay_payment_id"),
                (String) body.get("razorpay_signature"),
                courseIds,
                userId
        ));
    }
}
