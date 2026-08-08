package com.studynotion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Async
    public void sendOtpEmail(String toEmail, String otp) {
        String subject = "StudyNotion - Email Verification OTP";
        String body = buildOtpEmailHtml(otp);
        sendEmail(toEmail, subject, body);
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String firstName) {
        String subject = "Welcome to StudyNotion!";
        String body = "<h2>Welcome, " + firstName + "!</h2>"
                + "<p>Your account has been created successfully on StudyNotion.</p>"
                + "<p>Start exploring thousands of courses today!</p>";
        sendEmail(toEmail, subject, body);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String token, String firstName) {
        String resetLink = frontendUrl + "/update-password/" + token;
        String subject = "StudyNotion - Reset Your Password";
        String body = "<h2>Hello, " + firstName + "</h2>"
                + "<p>You requested to reset your password.</p>"
                + "<p>Click the link below (valid for 15 minutes):</p>"
                + "<a href='" + resetLink + "' style='background:#FFD60A;padding:10px 20px;"
                + "text-decoration:none;color:#000;border-radius:5px;'>Reset Password</a>"
                + "<p>If you did not request this, ignore this email.</p>";
        sendEmail(toEmail, subject, body);
    }

    @Async
    public void sendPasswordChangedEmail(String toEmail, String firstName) {
        String subject = "StudyNotion - Password Updated";
        String body = "<h2>Hello, " + firstName + "</h2>"
                + "<p>Your password has been updated successfully.</p>"
                + "<p>If you did not make this change, please contact support immediately.</p>";
        sendEmail(toEmail, subject, body);
    }

    @Async
    public void sendCourseEnrollmentEmail(String toEmail, String firstName, String courseName) {
        String subject = "StudyNotion - Successfully Enrolled in " + courseName;
        String body = "<h2>Congratulations, " + firstName + "!</h2>"
                + "<p>You have successfully enrolled in <strong>" + courseName + "</strong>.</p>"
                + "<p>Start learning now and achieve your goals!</p>"
                + "<a href='" + frontendUrl + "/dashboard/enrolled-courses' "
                + "style='background:#FFD60A;padding:10px 20px;text-decoration:none;"
                + "color:#000;border-radius:5px;'>Go to My Courses</a>";
        sendEmail(toEmail, subject, body);
    }

    @Async
    public void sendPaymentSuccessEmail(String toEmail, String firstName,
                                        String orderId, Double amount) {
        String subject = "StudyNotion - Payment Successful";
        String body = "<h2>Payment Confirmed, " + firstName + "!</h2>"
                + "<p>Your payment of <strong>₹" + amount + "</strong> was successful.</p>"
                + "<p>Order ID: " + orderId + "</p>"
                + "<p>Thank you for investing in your education!</p>";
        sendEmail(toEmail, subject, body);
    }

    private void sendEmail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "StudyNotion");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildOtpEmailHtml(String otp) {
        return "<div style='font-family:Arial;max-width:600px;margin:auto'>"
                + "<h1 style='color:#FFD60A'>StudyNotion</h1>"
                + "<h2>Email Verification</h2>"
                + "<p>Your OTP for email verification is:</p>"
                + "<div style='background:#f4f4f4;padding:20px;text-align:center;"
                + "border-radius:10px;margin:20px 0'>"
                + "<h1 style='letter-spacing:10px;color:#333'>" + otp + "</h1>"
                + "</div>"
                + "<p>This OTP is valid for <strong>5 minutes</strong>.</p>"
                + "<p>If you did not request this, ignore this email.</p>"
                + "</div>";
    }
}
