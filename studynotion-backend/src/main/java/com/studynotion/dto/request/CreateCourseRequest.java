package com.studynotion.dto.request;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Multipart form payload from the course-information step.
 *
 * The React app sends {@code tag} and {@code instructions} as JSON-encoded strings
 * (e.g. {@code ["java","spring"]}), so they are received as raw {@link String} and
 * decoded on demand. {@code categoryId} is a String to tolerate both numeric ids
 * and legacy Mongo ObjectIds.
 */
@Data
public class CreateCourseRequest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String courseName;
    private String courseDescription;
    private String whatYouWillLearn;
    private Double price;

    private String tag;
    private String instructions;

    private String categoryId;
    private String status;
    private String thumbnail;

    public List<String> getTag() {
        return parseList(tag);
    }

    public List<String> getInstructions() {
        return parseList(instructions);
    }

    private static List<String> parseList(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String trimmed = raw.trim();
        if (trimmed.startsWith("[")) {
            try {
                return MAPPER.readValue(trimmed, new TypeReference<List<String>>() {});
            } catch (Exception ignored) {
                // fall through to CSV handling
            }
        }
        List<String> result = new ArrayList<>();
        for (String part : trimmed.split(",")) {
            String value = part.trim().replaceAll("^\"|\"$", "");
            if (!value.isEmpty()) result.add(value);
        }
        return result.isEmpty() ? null : result;
    }

    /** Unused fallback kept for clarity when a caller hands us a String[] form field. */
    public static List<String> fromArray(String[] values) {
        return values == null ? null : new ArrayList<>(Arrays.asList(values));
    }
}
