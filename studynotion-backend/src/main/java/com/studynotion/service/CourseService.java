
package com.studynotion.service;

import com.studynotion.dto.request.CreateCourseRequest;
import com.studynotion.dto.response.ApiResponse;
import com.studynotion.entity.*;
import com.studynotion.exception.AppException;
import com.studynotion.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SectionRepository sectionRepository;
    private final SubSectionRepository subSectionRepository;
    private final CloudinaryService cloudinaryService;
    private final AIService aiService;

    @Transactional
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ApiResponse<Map<String, Object>> createCourse(
            CreateCourseRequest request, MultipartFile thumbnail, Long instructorId) {

        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new AppException("Instructor not found", 404));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException("Category not found", 404));

        // Upload thumbnail to Cloudinary
        String thumbnailUrl = null;
        if (thumbnail != null && !thumbnail.isEmpty()) {
            thumbnailUrl = cloudinaryService.uploadImage(thumbnail, "studynotion/thumbnails");
        }

        Course.CourseStatus status = Course.CourseStatus.Draft;
        if (request.getStatus() != null) {
            try { status = Course.CourseStatus.valueOf(request.getStatus()); }
            catch (IllegalArgumentException ignored) {}
        }

        Course course = Course.builder()
                .courseName(request.getCourseName())
                .courseDescription(request.getCourseDescription())
                .instructor(instructor)
                .whatYouWillLearn(request.getWhatYouWillLearn())
                .price(request.getPrice())
                .tag(request.getTag() != null ? request.getTag() : new ArrayList<>())
                .category(category)
                .instructions(request.getInstructions() != null ? request.getInstructions() : new ArrayList<>())
                .thumbnail(thumbnailUrl)
                .status(status)
                .build();

        courseRepository.save(course);

        // Add course to instructor's courses
        instructor.getCourses().add(course);
        userRepository.save(instructor);

        log.info("Course created: {} by {}", course.getCourseName(), instructor.getEmail());

        return ApiResponse.success("Course created successfully", buildCourseMap(course));
    }

    @Transactional
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ApiResponse<Map<String, Object>> editCourse(
            Long courseId, CreateCourseRequest request,
            MultipartFile thumbnail, Long instructorId) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException("Course not found", 404));

        if (!course.getInstructor().getId().equals(instructorId)) {
            throw new AppException("You are not authorized to edit this course", 403);
        }

        if (request.getCourseName() != null) course.setCourseName(request.getCourseName());
        if (request.getCourseDescription() != null) course.setCourseDescription(request.getCourseDescription());
        if (request.getWhatYouWillLearn() != null) course.setWhatYouWillLearn(request.getWhatYouWillLearn());
        if (request.getPrice() != null) course.setPrice(request.getPrice());
        if (request.getTag() != null) course.setTag(request.getTag());
        if (request.getInstructions() != null) course.setInstructions(request.getInstructions());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException("Category not found", 404));
            course.setCategory(category);
        }

        if (request.getStatus() != null) {
            try { course.setStatus(Course.CourseStatus.valueOf(request.getStatus())); }
            catch (IllegalArgumentException ignored) {}
        }

        if (thumbnail != null && !thumbnail.isEmpty()) {
            String thumbnailUrl = cloudinaryService.uploadImage(thumbnail, "studynotion/thumbnails");
            course.setThumbnail(thumbnailUrl);
        }

        courseRepository.save(course);
        return ApiResponse.success("Course updated successfully", buildCourseMap(course));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> getAllCourses() {
        List<Course> courses = courseRepository.findByStatus(Course.CourseStatus.Published);
        List<Map<String, Object>> result = courses.stream()
                .map(this::buildCourseMap)
                .collect(Collectors.toList());
        return ApiResponse.success("Courses fetched successfully", result);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> getCourseDetails(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException("Course not found", 404));
        return ApiResponse.success("Course details fetched", buildCourseMap(course));
    }

    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> getFullCourseDetails(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException("Course not found", 404));

        // Build full course map with all content
        Map<String, Object> courseMap = buildFullCourseMap(course, userId);
        return ApiResponse.success("Full course details fetched", courseMap);
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> getInstructorCourses(Long instructorId) {
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new AppException("Instructor not found", 404));
        List<Course> courses = courseRepository.findByInstructor(instructor);
        List<Map<String, Object>> result = courses.stream()
                .map(this::buildCourseMap)
                .collect(Collectors.toList());
        return ApiResponse.success("Instructor courses fetched", result);
    }

    @Transactional
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ApiResponse<Void> deleteCourse(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException("Course not found", 404));
        courseRepository.delete(course);
        return ApiResponse.success("Course deleted successfully");
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> searchCourses(String query) {
        List<Course> courses = courseRepository.searchCourses(query);
        List<Map<String, Object>> result = courses.stream()
                .filter(c -> c.getStatus() == Course.CourseStatus.Published)
                .map(this::buildCourseMap)
                .collect(Collectors.toList());
        return ApiResponse.success("Search results", result);
    }

    // --- Section operations ---
    @Transactional
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ApiResponse<Map<String, Object>> addSection(Long courseId, String sectionName, Long instructorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException("Course not found", 404));

        if (!course.getInstructor().getId().equals(instructorId)) {
            throw new AppException("Not authorized", 403);
        }

        Section section = Section.builder()
                .sectionName(sectionName)
                .course(course)
                .build();
        sectionRepository.save(section);

        return ApiResponse.success("Section added successfully", buildCourseMap(course));
    }

    @Transactional
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ApiResponse<Map<String, Object>> updateSection(Long sectionId, String sectionName, Long instructorId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException("Section not found", 404));

        section.setSectionName(sectionName);
        sectionRepository.save(section);

        return ApiResponse.success("Section updated", buildCourseMap(section.getCourse()));
    }

    @Transactional
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ApiResponse<Void> deleteSection(Long sectionId, Long courseId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException("Section not found", 404));
        sectionRepository.delete(section);
        return ApiResponse.success("Section deleted successfully");
    }

    // --- SubSection operations ---
    @Transactional
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ApiResponse<Map<String, Object>> addSubSection(
            Long sectionId, String title, String description,
            MultipartFile video, Long instructorId) {

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException("Section not found", 404));

        String videoUrl = null;
        String duration = "0";
        if (video != null && !video.isEmpty()) {
            Map<String, Object> uploadResult = cloudinaryService.uploadVideo(video, "studynotion/videos");
            videoUrl = (String) uploadResult.get("secure_url");
            Object dur = uploadResult.get("duration");
            if (dur != null) duration = dur.toString();
        }

        SubSection subSection = SubSection.builder()
                .title(title)
                .description(description)
                .videoUrl(videoUrl)
                .timeDuration(duration)
                .section(section)
                .build();

        subSectionRepository.save(subSection);

        return ApiResponse.success("Lecture added successfully", buildCourseMap(section.getCourse()));
    }

    @Transactional
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ApiResponse<Map<String, Object>> updateSubSection(
            Long subSectionId, String title, String description,
            MultipartFile video) {

        SubSection sub = subSectionRepository.findById(subSectionId)
                .orElseThrow(() -> new AppException("Lecture not found", 404));

        if (title != null) sub.setTitle(title);
        if (description != null) sub.setDescription(description);
        if (video != null && !video.isEmpty()) {
            Map<String, Object> uploadResult = cloudinaryService.uploadVideo(video, "studynotion/videos");
            sub.setVideoUrl((String) uploadResult.get("secure_url"));
        }
        subSectionRepository.save(sub);

        return ApiResponse.success("Lecture updated", buildCourseMap(sub.getSection().getCourse()));
    }

    @Transactional
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ApiResponse<Void> deleteSubSection(Long subSectionId) {
        SubSection sub = subSectionRepository.findById(subSectionId)
                .orElseThrow(() -> new AppException("Lecture not found", 404));
        subSectionRepository.delete(sub);
        return ApiResponse.success("Lecture deleted successfully");
    }

    // ---- Helper: build course response maps ----
    public Map<String, Object> buildCourseMap(Course course) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", course.getId());
        map.put("courseName", course.getCourseName());
        map.put("courseDescription", course.getCourseDescription());
        map.put("price", course.getPrice());
        map.put("thumbnail", course.getThumbnail());
        map.put("status", course.getStatus());
        map.put("tag", course.getTag());
        map.put("whatYouWillLearn", course.getWhatYouWillLearn());
        map.put("instructions", course.getInstructions());
        map.put("studentsCount", course.getStudentsEnrolled().size());
        if (course.getInstructor() != null) {
            Map<String, Object> instructor = new HashMap<>();
            instructor.put("id", course.getInstructor().getId());
            instructor.put("firstName", course.getInstructor().getFirstName());
            instructor.put("lastName", course.getInstructor().getLastName());
            instructor.put("image", course.getInstructor().getImage());
            map.put("instructor", instructor);
        }
        if (course.getCategory() != null) {
            Map<String, Object> cat = new HashMap<>();
            cat.put("id", course.getCategory().getId());
            cat.put("name", course.getCategory().getName());
            map.put("category", cat);
        }
        // Sections list
        List<Map<String, Object>> sections = course.getCourseContent().stream()
                .map(this::buildSectionMap)
                .collect(Collectors.toList());
        map.put("courseContent", sections);
        return map;
    }

    private Map<String, Object> buildFullCourseMap(Course course, Long userId) {
        Map<String, Object> map = buildCourseMap(course);
        // All rating/review data
        double avg = course.getRatingAndReviews().stream()
                .mapToDouble(RatingAndReview::getRating)
                .average().orElse(0.0);
        map.put("avgRating", avg);
        map.put("totalRatings", course.getRatingAndReviews().size());
        return map;
    }

    private Map<String, Object> buildSectionMap(Section section) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", section.getId());
        map.put("sectionName", section.getSectionName());
        List<Map<String, Object>> subSections = section.getSubSections().stream()
                .map(this::buildSubSectionMap)
                .collect(Collectors.toList());
        map.put("subSections", subSections);
        return map;
    }

    private Map<String, Object> buildSubSectionMap(SubSection sub) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", sub.getId());
        map.put("title", sub.getTitle());
        map.put("description", sub.getDescription());
        map.put("videoUrl", sub.getVideoUrl());
        map.put("timeDuration", sub.getTimeDuration());
        return map;
    }
}