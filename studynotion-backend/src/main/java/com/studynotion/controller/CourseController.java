package com.studynotion.controller;

import com.studynotion.dto.request.CreateCourseRequest;
import com.studynotion.service.CategoryService;
import com.studynotion.service.CourseProgressService;
import com.studynotion.service.CourseService;
import com.studynotion.service.RatingService;
import com.studynotion.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final RatingService ratingService;
    private final CategoryService categoryService;
    private final CourseProgressService progressService;

    // ================== COURSE CRUD ==================

    @PostMapping("/createCourse")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createCourse(
            @Valid @ModelAttribute CreateCourseRequest request,
            @RequestPart(value = "thumbnailImage", required = false) MultipartFile thumbnail) {
        return ResponseEntity.ok(courseService.createCourse(
                request, thumbnail, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/editCourse")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> editCourse(
            @RequestParam Long courseId,
            @ModelAttribute CreateCourseRequest request,
            @RequestPart(value = "thumbnailImage", required = false) MultipartFile thumbnail) {
        return ResponseEntity.ok(courseService.editCourse(
                courseId, request, thumbnail, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/getAllCourses")
    public ResponseEntity<?> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @PostMapping("/getCourseDetails")
    public ResponseEntity<?> getCourseDetails(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(courseService.getCourseDetails(asLong(body.get("courseId"))));
    }

    @PostMapping("/getFullCourseDetails")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getFullCourseDetails(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(courseService.getFullCourseDetails(
                asLong(body.get("courseId")), SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/getInstructorCourses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getInstructorCourses() {
        return ResponseEntity.ok(courseService.getInstructorCourses(SecurityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/deleteCourse")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteCourse(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(courseService.deleteCourse(
                asLong(body.get("courseId")), SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/searchCourse")
    public ResponseEntity<?> searchCourse(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(courseService.searchCourses(body.get("searchQuery")));
    }

    // ================== SECTION ==================

    @PostMapping("/addSection")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addSection(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(courseService.addSection(
                asLong(body.get("courseId")),
                (String) body.get("sectionName"),
                SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/updateSection")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateSection(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(courseService.updateSection(
                asLong(body.get("sectionId")),
                (String) body.get("sectionName"),
                SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/deleteSection")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteSection(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(courseService.deleteSection(
                asLong(body.get("sectionId")),
                asLong(body.get("courseId")),
                SecurityUtils.getCurrentUserId()));
    }

    // ================== SUBSECTION ==================

    @PostMapping("/addSubSection")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addSubSection(
            @RequestParam("sectionId") Long sectionId,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestPart(value = "video", required = false) MultipartFile video) {
        return ResponseEntity.ok(courseService.addSubSection(
                sectionId, title, description, video, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/updateSubSection")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateSubSection(
            @RequestParam("subSectionId") Long subSectionId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestPart(value = "video", required = false) MultipartFile video) {
        return ResponseEntity.ok(courseService.updateSubSection(
                subSectionId, title, description, video, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/deleteSubSection")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteSubSection(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(courseService.deleteSubSection(
                asLong(body.get("subSectionId")), SecurityUtils.getCurrentUserId()));
    }

    // ================== PROGRESS ==================

    @PostMapping("/updateCourseProgress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> markLectureAsComplete(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(progressService.markLectureComplete(
                SecurityUtils.getCurrentUserId(),
                asLong(body.get("courseId")),
                asLong(body.get("subSectionId"))));
    }

    // ================== CATEGORIES ==================

    @PostMapping("/createCategory")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createCategory(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(categoryService.createCategory(
                body.get("name"), body.get("description")));
    }

    @GetMapping("/showAllCategories")
    public ResponseEntity<?> showAllCategories() {
        return ResponseEntity.ok(categoryService.showAllCategories());
    }

    @PostMapping("/getCategoryPageDetails")
    public ResponseEntity<?> getCategoryPageDetails(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(categoryService.getCategoryPageDetails(asLong(body.get("categoryId"))));
    }

    // ================== RATINGS ==================

    @PostMapping("/createRating")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createRating(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ratingService.createRating(
                asLong(body.get("courseId")),
                Double.valueOf(body.get("rating").toString()),
                (String) body.get("review"),
                SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/getAverageRating")
    public ResponseEntity<?> getAverageRating(@RequestParam Long courseId) {
        return ResponseEntity.ok(ratingService.getAverageRating(courseId));
    }

    @GetMapping("/getReviews")
    public ResponseEntity<?> getAllRatings() {
        return ResponseEntity.ok(ratingService.getAllRatings());
    }

    /** Jackson decodes bare JSON numbers in a {@code Map<String,Object>} as Integer;
     *  the React app also sometimes sends ids as strings. Normalise both to Long. */
    private static Long asLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        String s = value.toString().trim();
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            String tail = s.length() > 12 ? s.substring(s.length() - 12) : s;
            return Math.abs(Long.parseLong(tail, 16));
        }
    }
}
