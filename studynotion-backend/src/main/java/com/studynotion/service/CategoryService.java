package com.studynotion.service;

import com.studynotion.dto.response.ApiResponse;
import com.studynotion.entity.Category;
import com.studynotion.entity.Course;
import com.studynotion.exception.AppException;
import com.studynotion.repository.CategoryRepository;
import com.studynotion.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public ApiResponse<Map<String, Object>> createCategory(String name, String description) {
        if (categoryRepository.findByName(name).isPresent()) {
            throw new AppException("Category already exists", 400);
        }
        Category category = Category.builder()
                .name(name)
                .description(description)
                .build();
        categoryRepository.save(category);
        return ApiResponse.success("Category created",
                Map.of("id", category.getId(), "name", name));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> showAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        List<Map<String, Object>> result = categories.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("description", c.getDescription());
            // Count published courses safely without triggering lazy load issues
            try {
                long count = courseRepository.findByCategoryIdAndPublished(c.getId()).size();
                m.put("courseCount", count);
            } catch (Exception e) {
                m.put("courseCount", 0);
            }
            return m;
        }).collect(Collectors.toList());
        return ApiResponse.success("Categories fetched", result);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> getCategoryPageDetails(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException("Category not found", 404));

        List<Map<String, Object>> selectedCourses = courseRepository
                .findByCategoryIdAndPublished(categoryId)
                .stream().map(this::buildCourseSummary)
                .collect(Collectors.toList());

        List<Map<String, Object>> differentCourses = courseRepository
                .findByStatus(Course.CourseStatus.Published).stream()
                .filter(c -> c.getCategory() == null
                        || !c.getCategory().getId().equals(categoryId))
                .limit(10)
                .map(this::buildCourseSummary)
                .collect(Collectors.toList());

        List<Map<String, Object>> topCourses = courseRepository
                .findByStatus(Course.CourseStatus.Published).stream()
                .sorted(Comparator.comparingInt(
                        c -> -c.getStudentsEnrolled().size()))
                .limit(10)
                .map(this::buildCourseSummary)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("selectedCourses", selectedCourses);
        result.put("differentCourses", differentCourses);
        result.put("mostSellingCourses", topCourses);
        result.put("categoryName", category.getName());
        result.put("categoryDescription", category.getDescription());

        return ApiResponse.success("Category details fetched", result);
    }

    private Map<String, Object> buildCourseSummary(Course c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("courseName", c.getCourseName());
        m.put("courseDescription", c.getCourseDescription());
        m.put("thumbnail", c.getThumbnail());
        m.put("price", c.getPrice());
        try {
            m.put("studentsEnrolled", c.getStudentsEnrolled().size());
        } catch (Exception e) {
            m.put("studentsEnrolled", 0);
        }
        if (c.getInstructor() != null) {
            m.put("instructorName", c.getInstructor().getFirstName()
                    + " " + c.getInstructor().getLastName());
        }
        return m;
    }
}
