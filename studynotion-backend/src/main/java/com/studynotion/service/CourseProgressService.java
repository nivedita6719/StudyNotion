package com.studynotion.service;

import com.studynotion.dto.response.ApiResponse;
import com.studynotion.entity.*;
import com.studynotion.exception.AppException;
import com.studynotion.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CourseProgressService {

    private final CourseProgressRepository progressRepository;
    private final SubSectionRepository subSectionRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public ApiResponse<Map<String, Object>> markLectureComplete(
            Long userId, Long courseId, Long subSectionId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", 404));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException("Course not found", 404));

        SubSection subSection = subSectionRepository.findById(subSectionId)
                .orElseThrow(() -> new AppException("Lecture not found", 404));

        // Get or create progress record
        CourseProgress progress = progressRepository
                .findByUserIdAndCourseId(userId, courseId)
                .orElseGet(() -> {
                    CourseProgress p = CourseProgress.builder()
                            .user(user).course(course).build();
                    return progressRepository.save(p);
                });

        // Add to completed if not already
        if (!progress.getCompletedVideos().contains(subSection)) {
            progress.getCompletedVideos().add(subSection);
            progressRepository.save(progress);
        }

        // Calculate percentage
        int totalVideos = course.getCourseContent().stream()
                .mapToInt(s -> s.getSubSections().size()).sum();
        int completedCount = progress.getCompletedVideos().size();
        double percentage = totalVideos > 0
                ? Math.round((double) completedCount / totalVideos * 100.0) : 0;

        return ApiResponse.success("Lecture marked as complete",
                Map.of(
                        "courseId", courseId,
                        "completedVideos", completedCount,
                        "totalVideos", totalVideos,
                        "progressPercentage", percentage
                ));
    }
}
