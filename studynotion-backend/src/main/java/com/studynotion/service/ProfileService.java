
package com.studynotion.service;

import com.studynotion.dto.response.ApiResponse;
import com.studynotion.dto.response.UserResponse;
import com.studynotion.entity.*;
import com.studynotion.exception.AppException;
import com.studynotion.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseProgressRepository progressRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional(readOnly = true)
    public ApiResponse<UserResponse> getUserDetails(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", 404));
        return ApiResponse.success("User details fetched", UserResponse.from(user));
    }

    @Transactional
    public ApiResponse<UserResponse> updateProfile(Long userId,
                                                   Map<String, String> updates) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", 404));

        if (updates.containsKey("firstName"))
            user.setFirstName(updates.get("firstName"));
        if (updates.containsKey("lastName"))
            user.setLastName(updates.get("lastName"));
        if (updates.containsKey("gender"))
            user.setGender(updates.get("gender"));
        if (updates.containsKey("dateOfBirth"))
            user.setDateOfBirth(updates.get("dateOfBirth"));
        if (updates.containsKey("about"))
            user.setAbout(updates.get("about"));
        if (updates.containsKey("contactNumber"))
            user.setContactNumber(updates.get("contactNumber"));

        userRepository.save(user);
        return ApiResponse.success("Profile updated successfully",
                UserResponse.from(user));
    }

    @Transactional
    public ApiResponse<UserResponse> updateProfilePicture(Long userId,
                                                          MultipartFile image) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", 404));
        String imageUrl = cloudinaryService.uploadImage(image,
                "studynotion/profiles");
        user.setImage(imageUrl);
        userRepository.save(user);
        return ApiResponse.success("Profile picture updated",
                UserResponse.from(user));
    }

    @Transactional
    public ApiResponse<Void> deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", 404));
        userRepository.delete(user);
        return ApiResponse.success("Account deleted successfully");
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> getEnrolledCourses(
            Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", 404));

        List<Map<String, Object>> enrolledCourses = user.getCourses()
                .stream().map(course -> {
                    Map<String, Object> courseData = new LinkedHashMap<>();
                    courseData.put("id", course.getId());
                    courseData.put("courseName", course.getCourseName());
                    courseData.put("thumbnail", course.getThumbnail());
                    courseData.put("courseDescription",
                            course.getCourseDescription());

                    Optional<CourseProgress> progress =
                            progressRepository.findByUserIdAndCourseId(
                                    userId, course.getId());

                    int totalVideos = 0;
                    try {
                        totalVideos = course.getCourseContent().stream()
                                .mapToInt(s -> s.getSubSections().size())
                                .sum();
                    } catch (Exception ignored) {}

                    int completedVideos = progress
                            .map(p -> {
                                try {
                                    return p.getCompletedVideos().size();
                                } catch (Exception e) {
                                    return 0;
                                }
                            }).orElse(0);

                    double progressPercent = totalVideos > 0
                            ? Math.round(
                            (double) completedVideos / totalVideos * 100)
                            : 0;

                    courseData.put("progressPercentage", progressPercent);
                    courseData.put("totalVideos", totalVideos);
                    courseData.put("completedVideos", completedVideos);

                    return courseData;
                }).collect(Collectors.toList());

        return ApiResponse.success("Enrolled courses fetched", enrolledCourses);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> getInstructorDashboard(
            Long instructorId) {
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new AppException("Instructor not found",
                        404));

        List<Course> instructorCourses = courseRepository.findByInstructor(instructor);
        List<Map<String, Object>> courseStats = instructorCourses
                .stream().map(course -> {
                    Map<String, Object> stat = new LinkedHashMap<>();
                    stat.put("id", course.getId());
                    stat.put("courseName", course.getCourseName());
                    stat.put("thumbnail", course.getThumbnail());
                    stat.put("price", course.getPrice());
                    stat.put("status", course.getStatus());

                    int enrolled = 0;
                    try {
                        enrolled = course.getStudentsEnrolled().size();
                    } catch (Exception ignored) {}
                    stat.put("studentsEnrolled", enrolled);

                    double revenue = course.getPrice() != null
                            ? course.getPrice() * enrolled : 0;
                    stat.put("revenue", revenue);

                    double avgRating = 0;
                    try {
                        avgRating = course.getRatingAndReviews().stream()
                                .mapToDouble(RatingAndReview::getRating)
                                .average().orElse(0.0);
                    } catch (Exception ignored) {}
                    stat.put("avgRating",
                            Math.round(avgRating * 10.0) / 10.0);

                    return stat;
                }).collect(Collectors.toList());

        double totalRevenue = courseStats.stream()
                .mapToDouble(s -> (double) s.get("revenue")).sum();
        int totalStudents = courseStats.stream()
                .mapToInt(s -> (int) s.get("studentsEnrolled")).sum();

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("courses", courseStats);
        dashboard.put("totalCourses", instructorCourses.size());
        dashboard.put("totalStudents", totalStudents);
        dashboard.put("totalRevenue", totalRevenue);

        return ApiResponse.success("Dashboard data fetched", dashboard);
    }
}
