package com.studynotion.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight liveness/readiness probe used by Render's health check and the
 * docker-compose healthcheck. Kept dependency-free (no Actuator) on purpose.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping({"/health", "/healthz"})
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            body.put("db", "UP");
        } catch (Exception e) {
            body.put("db", "DOWN");
        }
        return body;
    }
}
