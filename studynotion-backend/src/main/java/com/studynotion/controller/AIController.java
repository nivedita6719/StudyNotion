package com.studynotion.controller;

import com.studynotion.service.AIService;
import com.studynotion.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()") // AI calls consume paid quota — never anonymous
public class AIController {

    private final AIService aiService;

    /**
     * Feature 1: Generate AI course summary
     * Called by instructor when creating/editing a course
     */
    @PostMapping("/generate-summary")
    public ResponseEntity<?> generateCourseSummary(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> topics = (List<String>) body.get("topics");
        return ResponseEntity.ok(aiService.generateCourseSummary(
                (String) body.get("courseTitle"),
                topics,
                (String) body.getOrDefault("level", "Beginner")
        ));
    }

    /**
     * Feature 2: Smart course recommendations for current student
     */
    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendations() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(aiService.getSmartRecommendations(userId));
    }

    /**
     * Feature 3: Course chatbot - ask a question about a specific course
     */
    @PostMapping("/chat/{courseId}")
    public ResponseEntity<?> askChatbot(
            @PathVariable Long courseId,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) body.get("chatHistory");
        return ResponseEntity.ok(aiService.askCourseChatbot(
                courseId,
                (String) body.get("question"),
                history
        ));
    }

    /**
     * Feature 4: Auto-generate quiz from a section
     */
    @PostMapping("/generate-quiz")
    public ResponseEntity<?> generateQuiz(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(aiService.generateQuiz(
                Long.valueOf(body.get("sectionId").toString()),
                (String) body.getOrDefault("difficulty", "Medium"),
                Integer.parseInt(body.getOrDefault("count", 5).toString())
        ));
    }
}
