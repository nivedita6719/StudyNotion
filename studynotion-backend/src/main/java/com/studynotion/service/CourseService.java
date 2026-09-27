package com.studynotion.service;

import com.studynotion.dto.request.CreateCourseRequest;
import com.studynotion.dto.response.ApiResponse;
import com.studynotion.entity.*;
import com.studynotion.exception.AppException;
import com.studynotion.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * All read helpers ({@link #buildCourseMap}, {@link #buildSectionMap}, ...) fully
 * materialise every lazy association into detached collections BEFORE the method
 * returns. Combined with {@code spring.jpa.open-in-view=false} this guarantees the
 * JSON serializer never touches a Hibernate proxy outside of a transaction.
 *
 * Role enforcement is done explicitly here (not via {@code @PreAuthorize("hasRole")})
 * because the account-type string and the Spring authority format historically
 * drifted apart. See {@link #requireInstructor(User)}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SectionRepository sectionRepository;
    private final SubSectionRepository subSectionRepository;
    private final CourseProgressRepository courseProgressRepository;
    private final CloudinaryService cloudinaryService;

    // ==================== COURSE CRUD ====================

    @Transactional
    public ApiResponse<Map<String, Object>> createCourse(
            CreateCourseRequest request, MultipartFile thumbnail, Long instructorId) {

        User instructor = requireUser(instructorId);
        requireInstructor(instructor);

        Category category = categoryRepository.findById(parseCategoryId(request.getCategoryId()))
                .orElseThrow(() -> new AppException("Category not found", 404));

        String thumbnailUrl = null;
        if (thumbnail != null && !thumbnail.isEmpty()) {
            thumbnailUrl = cloudinaryService.uploadImage(thumbnail, "studynotion/thumbnails");
        }

        Course course = Course.builder()
                .courseName(request.getCourseName())
                .courseDescription(request.getCourseDescription())
                .instructor(instructor)
                .whatYouWillLearn(request.getWhatYouWillLearn())
                .price(request.getPrice())
                .tag(request.getTag() != null ? new ArrayList<>(request.getTag()) : new ArrayList<>())
                .category(category)
                .instructions(request.getInstructions() != null ? new ArrayList<>(request.getInstructions()) : new ArrayList<>())
                .thumbnail(thumbnailUrl)
                .status(parseStatus(request.getStatus(), Course.CourseStatus.Draft))
                .build();

        course = courseRepository.save(course);

        // NOTE: do NOT add to instructor.getCourses() — that collection is the owning
        // side of the student-enrolment join table. Created courses are resolved via
        // Course.instructor / courseRepository.findByInstructor().

        log.info("Course created: {} by {}", course.getCourseName(), instructor.getEmail());
        return ApiResponse.success("Course created successfully", buildCourseMap(course));
    }

    @Transactional
    public ApiResponse<Map<String, Object>> editCourse(
            Long courseId, CreateCourseRequest request,
            MultipartFile thumbnail, Long instructorId) {

        Course course = requireCourse(courseId);
        requireOwnership(course, instructorId);

        if (request.getCourseName() != null) course.setCourseName(request.getCourseName());
        if (request.getCourseDescription() != null) course.setCourseDescription(request.getCourseDescription());
        if (request.getWhatYouWillLearn() != null) course.setWhatYouWillLearn(request.getWhatYouWillLearn());
        if (request.getPrice() != null) course.setPrice(request.getPrice());
        if (request.getTag() != null) course.setTag(new ArrayList<>(request.getTag()));
        if (request.getInstructions() != null) course.setInstructions(new ArrayList<>(request.getInstructions()));

        if (request.getCategoryId() != null && !request.getCategoryId().isBlank()) {
            Category category = categoryRepository.findById(parseCategoryId(request.getCategoryId()))
                    .orElseThrow(() -> new AppException("Category not found", 404));
            course.setCategory(category);
        }

        if (request.getStatus() != null) {
            course.setStatus(parseStatus(request.getStatus(), course.getStatus()));
        }

        if (thumbnail != null && !thumbnail.isEmpty()) {
            course.setThumbnail(cloudinaryService.uploadImage(thumbnail, "studynotion/thumbnails"));
        }

        courseRepository.save(course);
        return ApiResponse.success("Course updated successfully", buildCourseMap(course));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> getAllCourses() {
        List<Map<String, Object>> result = courseRepository.findByStatus(Course.CourseStatus.Published)
                .stream().map(this::buildCourseMap).collect(Collectors.toList());
        return ApiResponse.success("Courses fetched successfully", result);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> getCourseDetails(Long courseId) {
        Course course = requireCourse(courseId);
        return ApiResponse.success("Course details fetched", buildFullCourseMap(course, null));
    }

    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> getFullCourseDetails(Long courseId, Long userId) {
        Course course = requireCourse(courseId);
        Map<String, Object> courseDetails = buildFullCourseMap(course, userId);

        // The React ViewCourse/EditCourse pages expect { courseDetails, completedVideos, totalDuration }.
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("courseDetails", courseDetails);
        payload.put("completedVideos", courseDetails.get("completedVideos"));
        payload.put("totalDuration", courseDetails.get("totalDuration"));
        return ApiResponse.success("Full course details fetched", payload);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> getInstructorCourses(Long instructorId) {
        User instructor = requireUser(instructorId);
        List<Map<String, Object>> result = courseRepository.findByInstructor(instructor)
                .stream().map(this::buildCourseMap).collect(Collectors.toList());
        return ApiResponse.success("Instructor courses fetched", result);
    }

    @Transactional
    public ApiResponse<Void> deleteCourse(Long courseId, Long userId) {
        Course course = requireCourse(courseId);
        User user = requireUser(userId);
        boolean isOwner = course.getInstructor() != null
                && course.getInstructor().getId().equals(userId);
        if (!isOwner && user.getAccountType() != User.AccountType.Admin) {
            throw new AppException("You are not authorized to delete this course", 403);
        }

        // Detach the course from every enrolled student so the join rows go away.
        for (User student : new ArrayList<>(course.getStudentsEnrolled())) {
            student.getCourses().remove(course);
        }
        courseRepository.delete(course);
        return ApiResponse.success("Course deleted successfully");
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> searchCourses(String query) {
        if (query == null || query.isBlank()) {
            return ApiResponse.success("Search results", new ArrayList<>());
        }
        List<Map<String, Object>> result = courseRepository.searchCourses(query).stream()
                .filter(c -> c.getStatus() == Course.CourseStatus.Published)
                .map(this::buildCourseMap)
                .collect(Collectors.toList());
        return ApiResponse.success("Search results", result);
    }

    // ==================== SECTION ====================

    @Transactional
    public ApiResponse<Map<String, Object>> addSection(Long courseId, String sectionName, Long instructorId) {
        Course course = requireCourse(courseId);
        requireOwnership(course, instructorId);

        Section section = Section.builder()
                .sectionName(sectionName)
                .course(course)
                .build();
        section = sectionRepository.save(section);
        course.getCourseContent().add(section);

        return ApiResponse.success("Section added successfully", buildCourseMap(course));
    }

    @Transactional
    public ApiResponse<Map<String, Object>> updateSection(Long sectionId, String sectionName, Long instructorId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException("Section not found", 404));
        requireOwnership(section.getCourse(), instructorId);

        section.setSectionName(sectionName);
        sectionRepository.save(section);
        return ApiResponse.success("Section updated", buildCourseMap(section.getCourse()));
    }

    @Transactional
    public ApiResponse<Map<String, Object>> deleteSection(Long sectionId, Long courseId, Long instructorId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException("Section not found", 404));
        Course course = section.getCourse();
        requireOwnership(course, instructorId);

        course.getCourseContent().remove(section);
        sectionRepository.delete(section);
        return ApiResponse.success("Section deleted successfully", buildCourseMap(course));
    }

    // ==================== SUBSECTION ====================

    @Transactional
    public ApiResponse<Map<String, Object>> addSubSection(
            Long sectionId, String title, String description,
            MultipartFile video, Long instructorId) {

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException("Section not found", 404));
        requireOwnership(section.getCourse(), instructorId);

        if (video == null || video.isEmpty()) {
            throw new AppException("A lecture video is required", 400);
        }

        Map<String, Object> uploadResult = cloudinaryService.uploadVideo(video, "studynotion/videos");
        String videoUrl = (String) uploadResult.get("secure_url");
        String duration = formatDuration(uploadResult.get("duration"));

        SubSection subSection = SubSection.builder()
                .title(title)
                .description(description)
                .videoUrl(videoUrl)
                .timeDuration(duration)
                .section(section)
                .build();
        subSection = subSectionRepository.save(subSection);
        section.getSubSections().add(subSection);

        return ApiResponse.success("Lecture added successfully", buildCourseMap(section.getCourse()));
    }

    @Transactional
    public ApiResponse<Map<String, Object>> updateSubSection(
            Long subSectionId, String title, String description,
            MultipartFile video, Long instructorId) {

        SubSection sub = subSectionRepository.findById(subSectionId)
                .orElseThrow(() -> new AppException("Lecture not found", 404));
        requireOwnership(sub.getSection().getCourse(), instructorId);

        if (title != null && !title.isBlank()) sub.setTitle(title);
        if (description != null) sub.setDescription(description);
        if (video != null && !video.isEmpty()) {
            Map<String, Object> uploadResult = cloudinaryService.uploadVideo(video, "studynotion/videos");
            sub.setVideoUrl((String) uploadResult.get("secure_url"));
            sub.setTimeDuration(formatDuration(uploadResult.get("duration")));
        }
        subSectionRepository.save(sub);
        return ApiResponse.success("Lecture updated", buildCourseMap(sub.getSection().getCourse()));
    }

    @Transactional
    public ApiResponse<Map<String, Object>> deleteSubSection(Long subSectionId, Long instructorId) {
        SubSection sub = subSectionRepository.findById(subSectionId)
                .orElseThrow(() -> new AppException("Lecture not found", 404));
        Section section = sub.getSection();
        requireOwnership(section.getCourse(), instructorId);

        section.getSubSections().remove(sub);
        subSectionRepository.delete(sub);
        return ApiResponse.success("Lecture deleted successfully", buildCourseMap(section.getCourse()));
    }

    // ==================== RESPONSE BUILDERS ====================

    /**
     * Base course view. Every collection is copied into a plain {@link ArrayList}/
     * {@link LinkedHashMap} so nothing lazy escapes the transaction.
     */
    public Map<String, Object> buildCourseMap(Course course) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", course.getId());
        map.put("_id", course.getId()); // legacy key still referenced by the React app
        map.put("courseName", course.getCourseName());
        map.put("courseDescription", course.getCourseDescription());
        map.put("price", course.getPrice());
        map.put("thumbnail", course.getThumbnail());
        map.put("status", course.getStatus() != null ? course.getStatus().name() : null);
        map.put("tag", new ArrayList<>(course.getTag()));
        map.put("whatYouWillLearn", course.getWhatYouWillLearn());
        map.put("instructions", new ArrayList<>(course.getInstructions()));
        map.put("createdAt", course.getCreatedAt());

        List<User> students = course.getStudentsEnrolled();
        map.put("studentsEnrolled", students.stream().map(User::getId).collect(Collectors.toList()));
        map.put("studentsCount", students.size());

        if (course.getInstructor() != null) {
            User i = course.getInstructor();
            Map<String, Object> instructor = new LinkedHashMap<>();
            instructor.put("id", i.getId());
            instructor.put("_id", i.getId());
            instructor.put("firstName", i.getFirstName());
            instructor.put("lastName", i.getLastName());
            instructor.put("email", i.getEmail());
            instructor.put("image", i.getImage());
            map.put("instructor", instructor);
        }

        if (course.getCategory() != null) {
            Map<String, Object> cat = new LinkedHashMap<>();
            cat.put("id", course.getCategory().getId());
            cat.put("_id", course.getCategory().getId());
            cat.put("name", course.getCategory().getName());
            map.put("category", cat);
        }

        map.put("courseContent", course.getCourseContent().stream()
                .map(this::buildSectionMap).collect(Collectors.toList()));
        return map;
    }

    private Map<String, Object> buildFullCourseMap(Course course, Long userId) {
        Map<String, Object> map = buildCourseMap(course);

        List<RatingAndReview> reviews = course.getRatingAndReviews();
        double avg = reviews.stream().mapToDouble(RatingAndReview::getRating).average().orElse(0.0);
        map.put("avgRating", Math.round(avg * 10.0) / 10.0);
        map.put("totalRatings", reviews.size());
        map.put("ratingAndReviews", reviews.stream().map(r -> {
            Map<String, Object> rm = new LinkedHashMap<>();
            rm.put("id", r.getId());
            rm.put("_id", r.getId());
            rm.put("rating", r.getRating());
            rm.put("review", r.getReview());
            Map<String, Object> ru = new LinkedHashMap<>();
            ru.put("id", r.getUser().getId());
            ru.put("firstName", r.getUser().getFirstName());
            ru.put("lastName", r.getUser().getLastName());
            ru.put("image", r.getUser().getImage());
            rm.put("user", ru);
            return rm;
        }).collect(Collectors.toList()));

        int totalLectures = course.getCourseContent().stream()
                .mapToInt(s -> s.getSubSections().size()).sum();
        map.put("totalLectures", totalLectures);
        map.put("totalDuration", formatTotalDuration(course));

        if (userId != null) {
            List<Long> completed = courseProgressRepository.findByUserIdAndCourseId(userId, course.getId())
                    .map(p -> p.getCompletedVideos().stream().map(SubSection::getId).collect(Collectors.toList()))
                    .orElseGet(ArrayList::new);
            map.put("completedVideos", completed);
        } else {
            map.put("completedVideos", new ArrayList<>());
        }
        return map;
    }

    private Map<String, Object> buildSectionMap(Section section) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", section.getId());
        map.put("_id", section.getId());
        map.put("sectionName", section.getSectionName());
        List<Map<String, Object>> subs = section.getSubSections().stream()
                .map(this::buildSubSectionMap).collect(Collectors.toList());
        map.put("subSection", subs); // React app reads `subSection` (singular)
        map.put("subSections", subs);
        return map;
    }

    private Map<String, Object> buildSubSectionMap(SubSection sub) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", sub.getId());
        map.put("_id", sub.getId());
        map.put("title", sub.getTitle());
        map.put("description", sub.getDescription());
        map.put("videoUrl", sub.getVideoUrl());
        map.put("timeDuration", sub.getTimeDuration());
        return map;
    }

    // ==================== HELPERS ====================

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException("User not found", 404));
    }

    private Course requireCourse(Long id) {
        if (id == null) throw new AppException("courseId is required", 400);
        return courseRepository.findById(id)
                .orElseThrow(() -> new AppException("Course not found", 404));
    }

    private void requireInstructor(User user) {
        if (user.getAccountType() != User.AccountType.Instructor
                && user.getAccountType() != User.AccountType.Admin) {
            throw new AppException("Only instructors can perform this action", 403);
        }
    }

    private void requireOwnership(Course course, Long instructorId) {
        if (course == null || course.getInstructor() == null
                || !course.getInstructor().getId().equals(instructorId)) {
            throw new AppException("You are not authorized to modify this course", 403);
        }
    }

    /** Accepts a plain numeric id or a legacy Mongo ObjectId hex string. */
    Long parseCategoryId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new AppException("categoryId is required", 400);
        }
        String trimmed = raw.trim();
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException ignored) {
            // Old Mongo ObjectId — derive a stable positive long from its hex tail.
            try {
                String tail = trimmed.length() > 12 ? trimmed.substring(trimmed.length() - 12) : trimmed;
                return Math.abs(Long.parseLong(tail, 16));
            } catch (NumberFormatException e) {
                throw new AppException("Invalid categoryId: " + raw, 400);
            }
        }
    }

    private Course.CourseStatus parseStatus(String raw, Course.CourseStatus fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Course.CourseStatus.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /** Sums every lecture's mm:ss duration into a human "Xh Ym" / "Ym Zs" string. */
    private String formatTotalDuration(Course course) {
        int totalSeconds = 0;
        for (Section s : course.getCourseContent()) {
            for (SubSection sub : s.getSubSections()) {
                totalSeconds += parseDurationToSeconds(sub.getTimeDuration());
            }
        }
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int sec = totalSeconds % 60;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + sec + "s";
        return sec + "s";
    }

    private int parseDurationToSeconds(String mmss) {
        if (mmss == null || mmss.isBlank()) return 0;
        try {
            String[] parts = mmss.split(":");
            if (parts.length == 2) {
                return Integer.parseInt(parts[0].trim()) * 60 + Integer.parseInt(parts[1].trim());
            }
            return (int) Double.parseDouble(mmss.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Cloudinary returns duration in seconds (as a Number). Render as mm:ss. */
    private String formatDuration(Object rawSeconds) {
        if (rawSeconds == null) return "0:00";
        try {
            double seconds = Double.parseDouble(rawSeconds.toString());
            int total = (int) Math.round(seconds);
            return String.format("%d:%02d", total / 60, total % 60);
        } catch (NumberFormatException e) {
            return "0:00";
        }
    }
}
