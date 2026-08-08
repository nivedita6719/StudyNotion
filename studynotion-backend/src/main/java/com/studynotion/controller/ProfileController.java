
package com.studynotion.controller;

import com.studynotion.service.ProfileService;
import com.studynotion.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/getUserDetails")
    public ResponseEntity<?> getUserDetails() {
        return ResponseEntity.ok(profileService.getUserDetails(
                SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/updateProfile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> updates) {
        return ResponseEntity.ok(profileService.updateProfile(
                SecurityUtils.getCurrentUserId(), updates));
    }

    @PostMapping("/updateDisplayPicture")
    public ResponseEntity<?> updateProfilePicture(
            @RequestPart("displayPicture") MultipartFile image) {
        return ResponseEntity.ok(profileService.updateProfilePicture(
                SecurityUtils.getCurrentUserId(), image));
    }

    @DeleteMapping("/deleteProfile")
    public ResponseEntity<?> deleteProfile() {
        return ResponseEntity.ok(profileService.deleteAccount(
                SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/getEnrolledCourses")
    public ResponseEntity<?> getEnrolledCourses() {
        return ResponseEntity.ok(profileService.getEnrolledCourses(
                SecurityUtils.getCurrentUserId()));
    }

    // Removed @PreAuthorize — any authenticated user can call this
    // Role check happens inside service if needed
    @GetMapping("/instructorDashboard")
    public ResponseEntity<?> getInstructorDashboard() {
        return ResponseEntity.ok(profileService.getInstructorDashboard(
                SecurityUtils.getCurrentUserId()));
    }
}
