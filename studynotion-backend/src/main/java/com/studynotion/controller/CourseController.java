
package com.studynotion.controller;

import com.studynotion.dto.request.CreateCourseRequest;
import com.studynotion.service.CourseService;
import com.studynotion.service.RatingService;
import com.studynotion.service.CategoryService;
import com.studynotion.service.CourseProgressService;
import com.studynotion.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
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
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(courseService.createCourse(request, thumbnail, userId));
    }

    @PostMapping("/editCourse")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> editCourse(
            @RequestParam Long courseId,
            @ModelAttribute CreateCourseRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(courseService.editCourse(courseId, request, thumbnail, userId));
    }

    @GetMapping("/getAllCourses")
    public ResponseEntity<?> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @PostMapping("/getCourseDetails")
    public ResponseEntity<?> getCourseDetails(@RequestBody Map<String, Long> body) {
        return ResponseEntity.ok(courseService.getCourseDetails(body.get("courseId")));
    }

    @PostMapping("/getFullCourseDetails")
    public ResponseEntity<?> getFullCourseDetails(@RequestBody Map<String, Long> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(courseService.getFullCourseDetails(body.get("courseId"), userId));
    }

    @GetMapping("/getInstructorCourses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getInstructorCourses() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(courseService.getInstructorCourses(userId));
    }

    @DeleteMapping("/deleteCourse")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteCourse(@RequestParam Long courseId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(courseService.deleteCourse(courseId, userId));
    }

    @PostMapping("/searchCourse")
    public ResponseEntity<?> searchCourse(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(courseService.searchCourses(body.get("searchQuery")));
    }

    // ================== SECTION ==================

    @PostMapping("/addSection")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addSection(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(courseService.addSection(
                Long.valueOf(body.get("courseId").toString()),
                (String) body.get("sectionName"),
                userId
        ));
    }

    @PostMapping("/updateSection")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateSection(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(courseService.updateSection(
                Long.valueOf(body.get("sectionId").toString()),
                (String) body.get("sectionName"),
                userId
        ));
    }

    @PostMapping("/deleteSection")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteSection(@RequestBody Map<String, Long> body) {
        return ResponseEntity.ok(courseService.deleteSection(
                body.get("sectionId"), body.get("courseId")));
    }

    // ================== SUBSECTION ==================

    @PostMapping("/addSubSection")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addSubSection(
            @RequestParam Long sectionId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestPart(value = "video", required = false) MultipartFile video) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(courseService.addSubSection(
                sectionId, title, description, video, userId));
    }

    @PostMapping("/updateSubSection")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateSubSection(
            @RequestParam Long subSectionId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestPart(value = "video", required = false) MultipartFile video) {
        return ResponseEntity.ok(courseService.updateSubSection(
                subSectionId, title, description, video));
    }

    @PostMapping("/deleteSubSection")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteSubSection(@RequestBody Map<String, Long> body) {
        return ResponseEntity.ok(courseService.deleteSubSection(body.get("subSectionId")));
    }

    // ================== PROGRESS ==================

    @PostMapping("/updateCourseProgress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> markLectureAsComplete(@RequestBody Map<String, Long> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(progressService.markLectureComplete(
                userId, body.get("courseId"), body.get("subSectionId")));
    }

    // ================== CATEGORIES ==================

    @PostMapping("/createCategory")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INSTRUCTOR')")
    public ResponseEntity<?> createCategory(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(categoryService.createCategory(
                body.get("name"), body.get("description")));
    }

    @GetMapping("/showAllCategories")
    public ResponseEntity<?> showAllCategories() {
        return ResponseEntity.ok(categoryService.showAllCategories());
    }

    @PostMapping("/getCategoryPageDetails")
    public ResponseEntity<?> getCategoryPageDetails(@RequestBody Map<String, Long> body) {
        return ResponseEntity.ok(categoryService.getCategoryPageDetails(body.get("categoryId")));
    }

    // ================== RATINGS ==================

    @PostMapping("/createRating")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createRating(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ratingService.createRating(
                Long.valueOf(body.get("courseId").toString()),
                Double.valueOf(body.get("rating").toString()),
                (String) body.get("review"),
                userId
        ));
    }

    @GetMapping("/getAverageRating")
    public ResponseEntity<?> getAverageRating(@RequestParam Long courseId) {
        return ResponseEntity.ok(ratingService.getAverageRating(courseId));
    }

    @GetMapping("/getReviews")
    public ResponseEntity<?> getAllRatings() {
        return ResponseEntity.ok(ratingService.getAllRatings());
    }
}
