package com.studynotion.service;

import com.studynotion.dto.response.ApiResponse;
import com.studynotion.entity.*;
import com.studynotion.exception.AppException;
import com.studynotion.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingAndReviewRepository ratingRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Transactional
    public ApiResponse<Map<String, Object>> createRating(
            Long courseId, Double rating, String review, Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", 404));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException("Course not found", 404));

        // Check if student is enrolled
        if (!course.getStudentsEnrolled().contains(user)) {
            throw new AppException("You must be enrolled to rate this course", 403);
        }

        // Check if already rated
        if (ratingRepository.findByUserIdAndCourseId(userId, courseId).isPresent()) {
            throw new AppException("You have already rated this course", 400);
        }

        RatingAndReview ratingAndReview = RatingAndReview.builder()
                .user(user)
                .course(course)
                .rating(rating)
                .review(review)
                .build();

        ratingRepository.save(ratingAndReview);

        Map<String, Object> result = new HashMap<>();
        result.put("id", ratingAndReview.getId());
        result.put("rating", rating);
        result.put("review", review);
        result.put("user", user.getFirstName() + " " + user.getLastName());

        return ApiResponse.success("Rating submitted successfully", result);
    }

    public ApiResponse<Map<String, Object>> getAverageRating(Long courseId) {
        double avg = ratingRepository.findAverageRatingByCourseId(courseId).orElse(0.0);
        Map<String, Object> result = new HashMap<>();
        result.put("averageRating", Math.round(avg * 10.0) / 10.0);
        result.put("courseId", courseId);
        return ApiResponse.success("Average rating fetched", result);
    }

    public ApiResponse<List<Map<String, Object>>> getAllRatings() {
        List<RatingAndReview> ratings = ratingRepository.findTopRatings();
        List<Map<String, Object>> result = ratings.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("rating", r.getRating());
            m.put("review", r.getReview());
            m.put("user", r.getUser().getFirstName() + " " + r.getUser().getLastName());
            m.put("userImage", r.getUser().getImage());
            m.put("courseName", r.getCourse().getCourseName());
            m.put("courseId", r.getCourse().getId());
            return m;
        }).collect(Collectors.toList());
        return ApiResponse.success("All ratings fetched", result);
    }
}
