package com.studynotion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studynotion.dto.response.ApiResponse;
import com.studynotion.entity.Course;
import com.studynotion.entity.User;
import com.studynotion.exception.AppException;
import com.studynotion.entity.Section;
import com.studynotion.repository.CourseRepository;
import com.studynotion.repository.SectionRepository;
import com.studynotion.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    // Native Gemini endpoint — works with new AQ. auth key format
    // Key goes in x-goog-api-key header (NOT as query param)
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    // ==========================================
    // FEATURE 1: Course Summary Generator
    // ==========================================
    public ApiResponse<Map<String, Object>> generateCourseSummary(
            String courseTitle, List<String> topics, String level) {

        String prompt = String.format("""
                You are an expert educational content writer.
                Generate a comprehensive course summary for:

                Course Title: %s
                Topics Covered: %s
                Level: %s

                Return ONLY a JSON object with these exact keys (no markdown, no code fences):
                {
                  "summary": "2-3 paragraph course description",
                  "whatYouWillLearn": ["point 1", "point 2", "point 3", "point 4", "point 5"],
                  "prerequisites": ["prereq 1", "prereq 2"],
                  "targetAudience": "who this course is for",
                  "tags": ["tag1", "tag2", "tag3"]
                }
                """,
                courseTitle,
                String.join(", ", topics),
                level
        );

        String aiResponse = callGeminiAPI(prompt);
        Map<String, Object> result = parseJsonResponse(aiResponse);
        return ApiResponse.success("Course summary generated", result);
    }

    // ==========================================
    // FEATURE 2: Smart Course Recommendations
    // ==========================================
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> getSmartRecommendations(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", 404));

        List<String> enrolledCourses = user.getCourses().stream()
                .map(Course::getCourseName)
                .collect(Collectors.toList());

        List<String> enrolledCategories = user.getCourses().stream()
                .filter(c -> c.getCategory() != null)
                .map(c -> c.getCategory().getName())
                .distinct()
                .collect(Collectors.toList());

        List<Course> allCourses = courseRepository.findByStatus(Course.CourseStatus.Published);
        List<Course> availableCourses = allCourses.stream()
                .filter(c -> !user.getCourses().contains(c))
                .collect(Collectors.toList());

        if (availableCourses.isEmpty()) {
            return ApiResponse.success("No new recommendations", new ArrayList<>());
        }

        String availableCoursesText = availableCourses.stream()
                .map(c -> "ID:" + c.getId() + " | " + c.getCourseName()
                        + " | Category: " + (c.getCategory() != null ? c.getCategory().getName() : "General")
                        + " | Tags: " + String.join(",", c.getTag()))
                .collect(Collectors.joining("\n"));

        String prompt = String.format("""
                You are a smart course recommendation engine for an LMS platform.

                Student's enrolled courses: %s
                Student's interested categories: %s

                Available courses to recommend from:
                %s

                Recommend the top 4 most relevant course IDs for this student based on their learning history.
                Return ONLY a JSON array of course IDs (numbers), for example: [1, 5, 12, 8]
                Do not include any explanation, markdown, or code fences, only the raw JSON array.
                """,
                enrolledCourses.isEmpty() ? "None yet (new student)" : String.join(", ", enrolledCourses),
                enrolledCategories.isEmpty() ? "Not determined yet" : String.join(", ", enrolledCategories),
                availableCoursesText
        );

        try {
            String aiResponse = callGeminiAPI(prompt);
            String cleaned = aiResponse.trim().replaceAll("[^\\[\\]0-9,\\s]", "");
            List<Long> recommendedIds = objectMapper.readValue(
                    cleaned, objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, Long.class));

            List<Map<String, Object>> recommendations = recommendedIds.stream()
                    .map(id -> availableCourses.stream()
                            .filter(c -> c.getId().equals(id))
                            .findFirst().orElse(null))
                    .filter(Objects::nonNull)
                    .map(course -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", course.getId());
                        m.put("courseName", course.getCourseName());
                        m.put("courseDescription", course.getCourseDescription());
                        m.put("thumbnail", course.getThumbnail());
                        m.put("price", course.getPrice());
                        m.put("instructor", course.getInstructor().getFirstName()
                                + " " + course.getInstructor().getLastName());
                        return m;
                    })
                    .collect(Collectors.toList());

            return ApiResponse.success("AI recommendations ready", recommendations);

        } catch (Exception e) {
            log.error("Recommendation parsing error: {}", e.getMessage());
            List<Map<String, Object>> fallback = availableCourses.stream()
                    .limit(4)
                    .map(c -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", c.getId());
                        m.put("courseName", c.getCourseName());
                        m.put("thumbnail", c.getThumbnail());
                        m.put("price", c.getPrice());
                        return m;
                    }).collect(Collectors.toList());
            return ApiResponse.success("Popular courses", fallback);
        }
    }

    // ==========================================
    // FEATURE 3: In-Course AI Chatbot
    // ==========================================
    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> askCourseChatbot(
            Long courseId, String question, List<Map<String, String>> chatHistory) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException("Course not found", 404));

        StringBuilder courseContext = new StringBuilder();
        courseContext.append("Course: ").append(course.getCourseName()).append("\n");
        courseContext.append("Description: ").append(course.getCourseDescription()).append("\n");
        courseContext.append("Topics:\n");

        course.getCourseContent().forEach(section -> {
            courseContext.append("- ").append(section.getSectionName()).append("\n");
            section.getSubSections().forEach(sub ->
                    courseContext.append("  * ").append(sub.getTitle())
                            .append(": ").append(sub.getDescription()).append("\n")
            );
        });

        StringBuilder history = new StringBuilder();
        if (chatHistory != null && !chatHistory.isEmpty()) {
            chatHistory.stream().limit(6).forEach(msg ->
                    history.append(msg.get("role")).append(": ")
                            .append(msg.get("content")).append("\n")
            );
        }

        String fullPrompt = String.format("""
                You are an intelligent course assistant for the course "%s" on StudyNotion LMS.
                Answer student questions based on the course content below.
                Be helpful, concise, and encouraging.
                If a question is not related to the course, politely redirect to course topics.

                COURSE CONTENT:
                %s

                %s
                Student question: %s
                """,
                course.getCourseName(),
                courseContext,
                history.length() > 0 ? "Previous conversation:\n" + history : "",
                question
        );

        String aiAnswer = callGeminiAPI(fullPrompt);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("answer", aiAnswer);
        response.put("courseId", courseId);
        response.put("courseName", course.getCourseName());

        return ApiResponse.success("AI response generated", response);
    }

    // ==========================================
    // FEATURE 4: Auto Quiz Generator
    // ==========================================
    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> generateQuiz(
            Long sectionId, String difficulty, int questionCount) {

        String sectionContent = getSectionContent(sectionId);

        if (sectionContent.isBlank()) {
            throw new AppException("No content found to generate quiz from", 400);
        }

        String prompt = String.format("""
                You are an expert quiz generator for educational content.

                Generate %d multiple-choice questions based on this content:
                %s

                Difficulty level: %s

                Return ONLY a valid JSON object in this exact format (no markdown, no code fences):
                {
                  "quizTitle": "Quiz title here",
                  "difficulty": "%s",
                  "questions": [
                    {
                      "id": 1,
                      "question": "Question text here?",
                      "options": ["Option A", "Option B", "Option C", "Option D"],
                      "correctAnswer": 0,
                      "explanation": "Why this answer is correct"
                    }
                  ]
                }

                correctAnswer is the 0-based index of the correct option.
                """,
                questionCount, sectionContent, difficulty, difficulty
        );

        String aiResponse = callGeminiAPI(prompt);
        Map<String, Object> quiz = parseJsonResponse(aiResponse);
        return ApiResponse.success("Quiz generated successfully", quiz);
    }

    // ==========================================
    // CORE: Gemini API Call
    // ==========================================
    // AQ. auth key → x-goog-api-key HEADER (NOT query param, NOT Bearer)
    // This is the only change from the old version
    // ==========================================
    private String callGeminiAPI(String prompt) {
        try {
            Map<String, Object> part = Map.of("text", prompt);
            Map<String, Object> content = Map.of("parts", List.of(part));
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(content),
                    "generationConfig", Map.of(
                            "temperature", 0.7,
                            "maxOutputTokens", 1024
                    )
            );

            String requestJson = objectMapper.writeValueAsString(requestBody);

            Request request = new Request.Builder()
                    .url(GEMINI_URL)
                    // ✅ AQ. key goes here — x-goog-api-key header
                    .addHeader("x-goog-api-key", geminiApiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestJson, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null
                        ? response.body().string() : "no body";

                if (!response.isSuccessful()) {
                    log.error("Gemini API error {}: {}", response.code(), responseBody);
                    throw new AppException("AI service unavailable: " + response.code(), 503);
                }

                JsonNode jsonNode = objectMapper.readTree(responseBody);

                // Response shape: candidates[0].content.parts[0].text
                JsonNode textNode = jsonNode
                        .path("candidates").get(0)
                        .path("content")
                        .path("parts").get(0)
                        .path("text");

                if (textNode.isMissingNode()) {
                    log.error("Unexpected Gemini response: {}", responseBody);
                    throw new AppException("AI service returned unexpected response", 503);
                }

                return textNode.asText();
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            throw new AppException("AI service error: " + e.getMessage(), 503);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String response) {
        try {
            String cleaned = response.trim();
            if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
            if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
            if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
            return objectMapper.readValue(cleaned.trim(), Map.class);
        } catch (Exception e) {
            log.error("Failed to parse AI JSON response: {}", e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("rawResponse", response);
            return fallback;
        }
    }

    private String getSectionContent(Long sectionId) {
        Section section = sectionRepository.findById(sectionId).orElse(null);
        if (section == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("Section: ").append(section.getSectionName()).append("\n");
        section.getSubSections().forEach(sub -> {
            sb.append("Lecture: ").append(sub.getTitle()).append("\n");
            if (sub.getDescription() != null) {
                sb.append("Content: ").append(sub.getDescription()).append("\n");
            }
        });
        return sb.toString();
    }
}
